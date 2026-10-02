package dev.portablepocketcabin;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.*;
import net.minecraft.world.ticks.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class RoomRelocationSpikeTest {
    private static final AtomicInteger NEXT=new AtomicInteger();
    private static final int RUN_BASE=(int)(System.currentTimeMillis()%10000)*16;
    record Fixture(ServerLevel level,BlockPos min,BlockPos max,Cow cow) {}
    static void whenReady(GameTestHelper h,Fixture f,Runnable action) {
        boolean[] done={false};
        h.onEachTick(()->{if(done[0])return;
            var box=new AABB(Vec3.atLowerCornerOf(f.min),Vec3.atLowerCornerOf(f.max.offset(1,1,1)));
            if(f.level.getEntities((Entity)null,box,e->e==f.cow).isEmpty())return;
            done[0]=true;action.run();
        });
    }
    static Fixture fixture(GameTestHelper h) {
        ServerLevel l=h.getLevel();
        int n=NEXT.incrementAndGet()+RUN_BASE;BlockPos min=new BlockPos(100015+n*64,200,100000+n*64),max=min.offset(5,3,5);
        for(int x=(min.getX()-3)>>4;x<=(max.getX()+3)>>4;x++)for(int z=(min.getZ()-3)>>4;z<=(max.getZ()+3)>>4;z++)l.setChunkForced(x,z,true);
        for(BlockPos p:BlockPos.betweenClosed(min.offset(-3,-3,-3),max.offset(3,3,3)))l.setBlock(p,Blocks.AIR.defaultBlockState(),RoomRelocationSpike.FLAGS);
        for(int x=0;x<6;x++)for(int z=0;z<6;z++)l.setBlock(min.offset(x,0,z),Blocks.STONE.defaultBlockState(),RoomRelocationSpike.FLAGS);
        l.setBlock(min.offset(0,1,1),Blocks.CHEST.defaultBlockState(),RoomRelocationSpike.FLAGS);
        ChestBlockEntity chest=(ChestBlockEntity)l.getBlockEntity(min.offset(0,1,1));
        ItemStack diamonds=new ItemStack(Items.DIAMOND,13);diamonds.set(DataComponents.CUSTOM_NAME,Component.literal("Keep me"));chest.setItem(0,diamonds);
        l.setBlock(min.offset(1,1,1),Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE,ChestType.LEFT),RoomRelocationSpike.FLAGS);
        l.setBlock(min.offset(2,1,1),Blocks.CHEST.defaultBlockState().setValue(ChestBlock.TYPE,ChestType.RIGHT),RoomRelocationSpike.FLAGS);
        ((ChestBlockEntity)l.getBlockEntity(min.offset(1,1,1))).setItem(0,new ItemStack(Items.EMERALD,7));
        ((ChestBlockEntity)l.getBlockEntity(min.offset(2,1,1))).setItem(0,new ItemStack(Items.GOLD_INGOT,11));
        l.setBlock(min.offset(3,1,1),Blocks.FURNACE.defaultBlockState(),RoomRelocationSpike.FLAGS);
        FurnaceBlockEntity f=(FurnaceBlockEntity)l.getBlockEntity(min.offset(3,1,1));f.setItem(0,new ItemStack(Items.RAW_IRON));f.setItem(1,new ItemStack(Items.COAL));
        for(int i=0;i<50;i++)AbstractFurnaceBlockEntity.serverTick(l,f.getBlockPos(),l.getBlockState(f.getBlockPos()),f);
        l.setBlock(min.offset(2,1,3),Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING,Direction.EAST).setValue(BedBlock.PART,BedPart.FOOT),RoomRelocationSpike.FLAGS);
        l.setBlock(min.offset(3,1,3),Blocks.BED.red().defaultBlockState().setValue(BedBlock.FACING,Direction.EAST).setValue(BedBlock.PART,BedPart.HEAD),RoomRelocationSpike.FLAGS);
        l.setBlock(min.offset(4,1,2),Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.LOWER),RoomRelocationSpike.FLAGS);
        l.setBlock(min.offset(4,2,2),Blocks.OAK_DOOR.defaultBlockState().setValue(DoorBlock.HALF,DoubleBlockHalf.UPPER),RoomRelocationSpike.FLAGS);
        l.setBlock(min.offset(0,2,4),Blocks.STONE.defaultBlockState(),RoomRelocationSpike.FLAGS);
        l.setBlock(min.offset(0,2,3),Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING,Direction.NORTH),RoomRelocationSpike.FLAGS);
        l.setBlock(min.offset(1,0,4),Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE,7),RoomRelocationSpike.FLAGS);
        l.setBlock(min.offset(1,1,4),Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE,4),RoomRelocationSpike.FLAGS);
        for(BlockPos p:List.of(min.offset(3,1,4),min.offset(5,1,4),min.offset(4,1,3),min.offset(4,1,5)))l.setBlock(p,Blocks.STONE.defaultBlockState(),RoomRelocationSpike.FLAGS);
        l.setBlock(min.offset(4,1,4),Blocks.WATER.defaultBlockState(),RoomRelocationSpike.FLAGS);
        l.setBlock(min.offset(5,1,1),Blocks.REPEATER.defaultBlockState(),RoomRelocationSpike.FLAGS);
        l.getBlockTicks().schedule(new ScheduledTick<>(Blocks.REPEATER,min.offset(5,1,1),l.getGameTime()+1000,TickPriority.HIGH,771));
        l.getFluidTicks().schedule(new ScheduledTick<>(Fluids.WATER,min.offset(4,1,4),l.getGameTime()+1000,TickPriority.NORMAL,772));
        for(var e:l.getEntities((Entity)null,new AABB(Vec3.atLowerCornerOf(min.offset(-3,-3,-3)),Vec3.atLowerCornerOf(max.offset(4,4,4))),e->true))e.discard();
        Cow cow=EntityTypes.COW.create(l,EntitySpawnReason.COMMAND);cow.setPos(min.getX()+2.5,min.getY()+1,min.getZ()+2.5);cow.setNoAi(true);cow.setCustomName(Component.literal("Room cow"));cow.setHealth(9);l.addFreshEntity(cow);
        return new Fixture(l,min,max,cow);
    }
    static void verify(GameTestHelper h,Fixture f,RoomRelocationSpike move) {
        for(var c:move.cells) {
            BlockPos dest=c.pos().offset(move.delta);
            h.assertTrue(f.level.getBlockState(dest).equals(c.state()),"Block state and orientation must survive at "+dest);
            if(c.data()!=null)h.assertTrue(f.level.getBlockEntity(dest).saveWithFullMetadata(f.level.registryAccess()).equals(relocated(c.data(),dest)),"Block-entity state must survive at "+dest);
        }
        h.assertTrue(f.level.getBlockEntity(f.min.offset(move.delta).offset(0,1,1)).getBlockPos().equals(f.min.offset(move.delta).offset(0,1,1)),"Block entity must belong to its new position");
        for(var c:move.cells)if(!net.minecraft.world.level.levelgen.structure.BoundingBox.fromCorners(move.min.offset(move.delta),move.max.offset(move.delta)).isInside(c.pos()))h.assertTrue(f.level.getBlockState(c.pos()).isAir(),"Old non-overlapping source must clear");
        for(var t:move.blocks)h.assertTrue(f.level.getBlockTicks().hasScheduledTick(t.pos().offset(move.delta),t.type()),"Pending block tick must move");
        for(var t:move.fluids)h.assertTrue(f.level.getFluidTicks().hasScheduledTick(t.pos().offset(move.delta),t.type()),"Pending fluid tick must move");
        var b=move.bodies.stream().filter(e->e.id().equals(f.cow.getUUID())).findFirst().orElseThrow();
        h.assertTrue(f.cow.position().distanceTo(new Vec3(b.x(),b.y(),b.z()))<0.001,"Same animal must move to absolute destination");
        h.assertTrue(f.cow.getHealth()==9 && f.cow.getCustomName().getString().equals("Room cow"),"Animal state must survive");
        h.assertTrue(f.level.getEntities((Entity)null,new AABB(Vec3.atLowerCornerOf(move.min.offset(move.delta)),Vec3.atLowerCornerOf(move.max.offset(move.delta).offset(1,1,1))),e->e.getUUID().equals(f.cow.getUUID())).size()==1,"Animal must exist once");
        h.assertTrue(f.level.getEntities(EntityTypes.ITEM,new AABB(Vec3.atLowerCornerOf(move.min.offset(-2,-2,-2)),Vec3.atLowerCornerOf(move.max.offset(3,3,3))),e->true).isEmpty(),"Relocation must not drop inventory items");
    }
    static CompoundTag relocated(CompoundTag original,BlockPos dest) { CompoundTag n=original.copy();n.putInt("x",dest.getX());n.putInt("y",dest.getY());n.putInt("z",dest.getZ());return n; }
    @GameTest(maxTicks=260) public void eastOverlapAcrossChunkBoundary(GameTestHelper h) { check(h,new BlockPos(1,0,0)); }
    @GameTest(maxTicks=260) public void westOverlapAcrossChunkBoundary(GameTestHelper h) { check(h,new BlockPos(-1,0,0)); }
    @GameTest(maxTicks=260) public void northOverlap(GameTestHelper h) { check(h,new BlockPos(0,0,-1)); }
    @GameTest(maxTicks=260) public void southOverlap(GameTestHelper h) { check(h,new BlockPos(0,0,1)); }
    static void check(GameTestHelper h,BlockPos d) { Fixture f=fixture(h);whenReady(h,f,()->{var move=RoomRelocationSpike.capture(f.level,f.min,f.max,d);move.apply(f.level,0);verify(h,f,move);h.succeed();}); }
    @GameTest(maxTicks=260) public void interruptionAfterClearRecovers(GameTestHelper h) { recover(h,1); }
    @GameTest(maxTicks=260) public void interruptionDuringWriteRecovers(GameTestHelper h) { recover(h,2); }
    @GameTest(maxTicks=260) public void interruptionAfterBlocksRecovers(GameTestHelper h) { recover(h,3); }
    @GameTest(maxTicks=260) public void interruptionAfterEntitiesRecovers(GameTestHelper h) { recover(h,4); }
    static void recover(GameTestHelper h,int stage) {
        Fixture f=fixture(h);whenReady(h,f,()->{var move=RoomRelocationSpike.capture(f.level,f.min,f.max,new BlockPos(1,0,0));
        try { Path journal=Files.createTempFile("room-move-", ".nbt");NbtIo.write(move.save(),journal);
            try { move.apply(f.level,stage);h.fail("Failpoint did not interrupt"); }catch(RoomRelocationSpike.InterruptedMove expected){}
            var restored=RoomRelocationSpike.load(NbtIo.read(journal));restored.apply(f.level,0);restored.apply(f.level,0);verify(h,f,restored);Files.delete(journal);h.succeed();
        }catch(Exception e){throw new RuntimeException(e);}});
    }
    @GameTest(maxTicks=260) public void blockedDestinationRejectsWithoutMutation(GameTestHelper h) {
        Fixture f=fixture(h);BlockPos blocker=f.max.offset(1,0,0);f.level.setBlock(blocker,Blocks.DIAMOND_BLOCK.defaultBlockState(),RoomRelocationSpike.FLAGS);
        var before=f.level.getBlockEntity(f.min.offset(0,1,1)).saveWithFullMetadata(f.level.registryAccess());
        try {RoomRelocationSpike.capture(f.level,f.min,f.max,new BlockPos(1,0,0));h.fail("Obstruction must reject");}catch(IllegalStateException expected){}
        h.assertTrue(f.level.getBlockState(blocker).is(Blocks.DIAMOND_BLOCK),"Blocker must remain");h.assertTrue(f.level.getBlockEntity(f.min.offset(0,1,1)).saveWithFullMetadata(f.level.registryAccess()).equals(before),"Inventory must remain");h.succeed();
    }
    @GameTest(maxTicks=260) public void movedFurnaceContinuesCooking(GameTestHelper h) {
        Fixture f=fixture(h);whenReady(h,f,()->{var move=RoomRelocationSpike.capture(f.level,f.min,f.max,new BlockPos(1,0,0));move.apply(f.level,0);verify(h,f,move);
        BlockPos dest=f.min.offset(4,1,1);
        h.succeedWhen(()->{FurnaceBlockEntity furnace=(FurnaceBlockEntity)f.level.getBlockEntity(dest);h.assertTrue(furnace.getItem(2).is(Items.IRON_INGOT),"Moved furnace must finish the in-progress recipe");h.assertTrue(furnace.getItem(2).getCount()==1,"Output must exist once");});});
    }
    @GameTest(maxTicks=180) public void hangingDecorationKeepsAttachment(GameTestHelper h) {
        Fixture f=fixture(h);BlockPos support=f.min.offset(4,2,4),anchor=f.min.offset(5,2,4);
        f.level.setBlock(support,Blocks.STONE.defaultBlockState(),RoomRelocationSpike.FLAGS);
        var frame=new net.minecraft.world.entity.decoration.ItemFrame(f.level,anchor,Direction.EAST);
        ItemStack item=new ItemStack(Items.DIAMOND);item.set(DataComponents.CUSTOM_NAME,Component.literal("Frame diamond"));frame.setItem(item,false);frame.setRotation(3);f.level.addFreshEntity(frame);
        whenReady(h,f,()->{
            h.assertTrue(frame.survives(),"Frame fixture must have valid support");
            var move=RoomRelocationSpike.capture(f.level,f.min,f.max,new BlockPos(1,0,0));move.apply(f.level,0);verify(h,f,move);
            h.runAfterDelay(120,()->{h.assertTrue(!frame.isRemoved() && frame.getPos().equals(anchor.offset(1,0,0)) && frame.survives(),"Moved frame must keep its attachment through survival checks: removed="+frame.isRemoved()+" anchor="+frame.getPos()+" expected="+anchor.offset(1,0,0)+" survives="+frame.survives());h.assertTrue(frame.getItem().is(Items.DIAMOND) && frame.getRotation()==3,"Frame item and rotation must survive");h.succeed();});
        });
    }
    @GameTest(maxTicks=260) public void vehiclePassengerRelationshipSurvives(GameTestHelper h) {
        Fixture f=fixture(h);var cart=EntityTypes.MINECART.create(f.level,EntitySpawnReason.COMMAND);cart.setPos(f.min.getX()+2.5,f.min.getY()+1,f.min.getZ()+2.5);f.level.addFreshEntity(cart);f.cow.startRiding(cart,true,false);
        whenReady(h,f,()->{h.assertTrue(f.cow.getVehicle()==cart,"Fixture animal must ride the cart");var move=RoomRelocationSpike.capture(f.level,f.min,f.max,new BlockPos(1,0,0));move.apply(f.level,0);h.assertTrue(f.cow.getVehicle()==cart && cart.getPassengers().contains(f.cow),"Room move must preserve passenger relationship");h.succeed();});
    }

}
