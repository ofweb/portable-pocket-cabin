package dev.portablepocketcabin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.ticks.*;
import java.util.*;

/** Feasibility spike only: does not integrate cabin upgrades or promise mod compatibility. */
final class RoomRelocationSpike {
    static final int FLAGS = Block.UPDATE_SKIP_ALL_SIDEEFFECTS | Block.UPDATE_CLIENTS;
    record Cell(BlockPos pos, BlockState state, CompoundTag data) {}
    record Body(UUID id, double x, double y, double z, float yaw, float pitch, UUID vehicle) {}
    final BlockPos min, max, delta;
    final List<Cell> cells;
    final List<ScheduledTick<Block>> blocks;
    final List<ScheduledTick<net.minecraft.world.level.material.Fluid>> fluids;
    final List<Body> bodies;

    private RoomRelocationSpike(BlockPos min, BlockPos max, BlockPos delta, List<Cell> cells,
        List<ScheduledTick<Block>> blocks, List<ScheduledTick<net.minecraft.world.level.material.Fluid>> fluids, List<Body> bodies) {
        this.min=min; this.max=max; this.delta=delta; this.cells=cells; this.blocks=blocks; this.fluids=fluids; this.bodies=bodies;
    }
    static RoomRelocationSpike capture(ServerLevel level, BlockPos min, BlockPos max, BlockPos delta) {
        BoundingBox box=BoundingBox.fromCorners(min,max);
        for(BlockPos p:BlockPos.betweenClosed(min.offset(delta),max.offset(delta))) {
            if(!box.isInside(p) && !level.getBlockState(p).isAir()) throw new IllegalStateException("Destination obstructed at "+p+" by "+level.getBlockState(p));
        }
        List<Cell> cells=new ArrayList<>();
        for(BlockPos p:BlockPos.betweenClosed(min,max)) {
            BlockEntity be=level.getBlockEntity(p);
            cells.add(new Cell(p.immutable(),level.getBlockState(p),be==null?null:be.saveWithFullMetadata(level.registryAccess()).copy()));
        }
        List<ScheduledTick<Block>> blocks=new ArrayList<>();
        List<ScheduledTick<net.minecraft.world.level.material.Fluid>> fluids=new ArrayList<>();
        for(int x=min.getX()>>4;x<=max.getX()>>4;x++) for(int z=min.getZ()>>4;z<=max.getZ()>>4;z++) {
            var chunk=level.getChunk(x,z);
            ((LevelChunkTicks<Block>)chunk.getBlockTicks()).getAll().filter(t->box.isInside(t.pos())).forEach(blocks::add);
            ((LevelChunkTicks<net.minecraft.world.level.material.Fluid>)chunk.getFluidTicks()).getAll().filter(t->box.isInside(t.pos())).forEach(fluids::add);
        }
        List<Body> bodies=level.getEntities((Entity)null,new AABB(Vec3.atLowerCornerOf(min),Vec3.atLowerCornerOf(max.offset(1,1,1))),e->true).stream()
            .map(e->new Body(e.getUUID(),e.getX()+delta.getX(),e.getY()+delta.getY(),e.getZ()+delta.getZ(),e.getYRot(),e.getXRot(),e.getVehicle()==null?null:e.getVehicle().getUUID())).toList();
        Set<UUID> bodyIds=new HashSet<>();for(Body b:bodies)bodyIds.add(b.id);
        for(Body b:bodies)if(b.vehicle!=null && !bodyIds.contains(b.vehicle))throw new IllegalStateException("Vehicle crosses move boundary");
        return new RoomRelocationSpike(min,max,delta,cells,blocks,fluids,bodies);
    }
    // Failpoints emulate interruptions. Replaying from a serialized snapshot uses absolute destinations.
    void apply(ServerLevel level, int failpoint) {
        Set<BlockPos> union=new LinkedHashSet<>();
        for(Cell c:cells) { union.add(c.pos); union.add(c.pos.offset(delta)); }
        level.getBlockTicks().clearArea(BoundingBox.fromCorners(min,max));
        level.getBlockTicks().clearArea(BoundingBox.fromCorners(min.offset(delta),max.offset(delta)));
        level.getFluidTicks().clearArea(BoundingBox.fromCorners(min,max));
        level.getFluidTicks().clearArea(BoundingBox.fromCorners(min.offset(delta),max.offset(delta)));
        for(BlockPos p:union) level.setBlock(p,Blocks.BARRIER.defaultBlockState(),FLAGS);
        for(BlockPos p:union) level.setBlock(p,Blocks.AIR.defaultBlockState(),FLAGS);
        if(failpoint==1) throw new InterruptedMove();
        int count=0;
        for(Cell c:cells) {
            BlockPos dest=c.pos.offset(delta);
            level.setBlock(dest,c.state,FLAGS);
            if(c.data!=null) {
                BlockEntity be=level.getBlockEntity(dest);
                if(be==null) throw new IllegalStateException("Missing target block entity");
                be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING,level.registryAccess(),c.data));
                be.setChanged();
            }
            if(failpoint==2 && ++count==cells.size()/2) throw new InterruptedMove();
        }
        if(failpoint==3) throw new InterruptedMove();
        for(var t:blocks) level.getBlockTicks().schedule(new ScheduledTick<>(t.type(),t.pos().offset(delta),t.triggerTick(),t.priority(),t.subTickOrder()));
        for(var t:fluids) level.getFluidTicks().schedule(new ScheduledTick<>(t.type(),t.pos().offset(delta),t.triggerTick(),t.priority(),t.subTickOrder()));
        for(Body b:bodies) {
            Entity e=level.getEntityInAnyDimension(b.id);
            if(e==null) throw new IllegalStateException("Missing entity during recovery");
            e.stopRiding();
            e.teleportTo(level,b.x,b.y,b.z,Set.of(),b.yaw,b.pitch,false);
        }
        for(Body b:bodies)if(b.vehicle!=null) {
            Entity passenger=level.getEntityInAnyDimension(b.id),vehicle=level.getEntityInAnyDimension(b.vehicle);
            if(!passenger.startRiding(vehicle,true,false))throw new IllegalStateException("Cannot restore passenger relationship");
        }
        if(failpoint==4) throw new InterruptedMove();
    }
    CompoundTag save() {
        CompoundTag root=new CompoundTag();
        root.putIntArray("min",new int[]{min.getX(),min.getY(),min.getZ()});
        root.putIntArray("max",new int[]{max.getX(),max.getY(),max.getZ()});
        root.putIntArray("delta",new int[]{delta.getX(),delta.getY(),delta.getZ()});
        ListTag list=new ListTag();
        for(Cell c:cells) {
            CompoundTag n=new CompoundTag();n.putIntArray("p",new int[]{c.pos.getX(),c.pos.getY(),c.pos.getZ()});
            n.put("state",BlockState.CODEC.encodeStart(NbtOps.INSTANCE,c.state).getOrThrow());
            if(c.data!=null)n.put("data",c.data.copy());list.add(n);
        }
        root.put("cells",list);
        ListTag ticks=new ListTag();
        for(var t:blocks)ticks.add(tickTag("block",BuiltInRegistries.BLOCK.getKey(t.type()).toString(),t.pos(),t.triggerTick(),t.priority(),t.subTickOrder()));
        for(var t:fluids)ticks.add(tickTag("fluid",BuiltInRegistries.FLUID.getKey(t.type()).toString(),t.pos(),t.triggerTick(),t.priority(),t.subTickOrder()));
        root.put("ticks",ticks);
        ListTag entities=new ListTag();
        for(Body b:bodies) { CompoundTag n=new CompoundTag();n.putString("id",b.id.toString());n.putDouble("x",b.x);n.putDouble("y",b.y);n.putDouble("z",b.z);n.putFloat("yaw",b.yaw);n.putFloat("pitch",b.pitch);if(b.vehicle!=null)n.putString("vehicle",b.vehicle.toString());entities.add(n); }
        root.put("entities",entities);return root;
    }
    static CompoundTag tickTag(String kind,String id,BlockPos p,long time,TickPriority priority,long order) {
        CompoundTag n=new CompoundTag();n.putString("kind",kind);n.putString("id",id);n.putIntArray("p",new int[]{p.getX(),p.getY(),p.getZ()});n.putLong("time",time);n.putInt("priority",priority.ordinal());n.putLong("order",order);return n;
    }
    static BlockPos pos(CompoundTag tag,String name) { int[] v=tag.getIntArray(name).orElseThrow();return new BlockPos(v[0],v[1],v[2]); }
    static RoomRelocationSpike load(CompoundTag root) {
        List<Cell> cells=new ArrayList<>();
        for(Tag tag:root.getListOrEmpty("cells")) { CompoundTag n=(CompoundTag)tag;cells.add(new Cell(pos(n,"p"),BlockState.CODEC.parse(NbtOps.INSTANCE,n.get("state")).getOrThrow(),n.getCompound("data").map(CompoundTag::copy).orElse(null))); }
        List<ScheduledTick<Block>> blocks=new ArrayList<>();
        List<ScheduledTick<net.minecraft.world.level.material.Fluid>> fluids=new ArrayList<>();
        for(Tag tag:root.getListOrEmpty("ticks")) {
            CompoundTag n=(CompoundTag)tag;Identifier id=Identifier.parse(n.getStringOr("id",""));
            BlockPos p=pos(n,"p");long time=n.getLongOr("time",0),order=n.getLongOr("order",0);TickPriority priority=TickPriority.values()[n.getIntOr("priority",0)];
            if(n.getStringOr("kind","").equals("block"))blocks.add(new ScheduledTick<>(BuiltInRegistries.BLOCK.getValue(id),p,time,priority,order));
            else fluids.add(new ScheduledTick<>(BuiltInRegistries.FLUID.getValue(id),p,time,priority,order));
        }
        List<Body> bodies=new ArrayList<>();
        for(Tag tag:root.getListOrEmpty("entities")) { CompoundTag n=(CompoundTag)tag;bodies.add(new Body(UUID.fromString(n.getStringOr("id","")),n.getDoubleOr("x",0),n.getDoubleOr("y",0),n.getDoubleOr("z",0),n.getFloatOr("yaw",0),n.getFloatOr("pitch",0),n.getString("vehicle").map(UUID::fromString).orElse(null))); }
        return new RoomRelocationSpike(pos(root,"min"),pos(root,"max"),pos(root,"delta"),cells,blocks,fluids,bodies);
    }
    static final class InterruptedMove extends RuntimeException {}
}
