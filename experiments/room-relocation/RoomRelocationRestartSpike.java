package dev.portablepocketcabin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import java.nio.file.*;

final class RoomRelocationRestartSpike {
    static final net.minecraft.server.level.TicketType TICKET=new net.minecraft.server.level.TicketType(net.minecraft.server.level.TicketType.NO_TIMEOUT,net.minecraft.server.level.TicketType.FLAG_LOADING|net.minecraft.server.level.TicketType.FLAG_SIMULATION|net.minecraft.server.level.TicketType.FLAG_KEEP_DIMENSION_ACTIVE);
    static final Path JOURNAL=Path.of("room-relocation-journal.nbt");
    static final BlockPos MIN=new BlockPos(100015,200,100000),MAX=MIN.offset(5,3,5),DELTA=new BlockPos(1,0,0);
    static Runnable pending; static int readyTick;
    static void later(MinecraftServer server,int ticks,Runnable task){pending=task;readyTick=server.getTickCount()+ticks;}
    static void start(MinecraftServer server) {
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(srv->{if(srv==server && pending!=null && srv.getTickCount()>=readyTick){Runnable task=pending;pending=null;task.run();}});
        later(server,10,()->{
            try {
                ServerLevel level=server.getLevel(PocketDimension.LEVEL_KEY);
                for(int x=MIN.getX()>>4;x<=MAX.offset(1,0,0).getX()>>4;x++)for(int z=MIN.getZ()>>4;z<=MAX.getZ()>>4;z++){level.getChunkSource().addTicketWithRadius(TICKET,new net.minecraft.world.level.ChunkPos(x,z),2);level.getChunk(x,z);}
                if(System.getProperty("portable-pocket-cabin.startup-test").equals("seed"))seed(server,level);
                else verify(server,level);
            }catch(Exception e){PortablePocketCabin.LOGGER.error("RELOCATION_RESTART_SPIKE_FAILED",e);server.halt(false);}
        });
    }
    static void seed(MinecraftServer server,ServerLevel l)throws Exception {
        for(BlockPos p:BlockPos.betweenClosed(MIN,MAX))l.setBlock(p,Blocks.AIR.defaultBlockState(),RoomRelocationSpike.FLAGS);
        for(int x=0;x<6;x++)for(int z=0;z<6;z++)l.setBlock(MIN.offset(x,0,z),Blocks.STONE.defaultBlockState(),RoomRelocationSpike.FLAGS);
        BlockPos chest=MIN.offset(0,1,1);l.setBlock(chest,Blocks.CHEST.defaultBlockState(),RoomRelocationSpike.FLAGS);
        ItemStack stack=new ItemStack(Items.DIAMOND,13);stack.set(DataComponents.CUSTOM_NAME,Component.literal("Restart diamonds"));((ChestBlockEntity)l.getBlockEntity(chest)).setItem(0,stack);
        BlockPos furnace=MIN.offset(3,1,1);l.setBlock(furnace,Blocks.FURNACE.defaultBlockState(),RoomRelocationSpike.FLAGS);
        FurnaceBlockEntity f=(FurnaceBlockEntity)l.getBlockEntity(furnace);f.setItem(0,new ItemStack(Items.RAW_IRON));f.setItem(1,new ItemStack(Items.COAL));
        for(int i=0;i<50;i++)AbstractFurnaceBlockEntity.serverTick(l,furnace,l.getBlockState(furnace),f);
        Cow cow=EntityTypes.COW.create(l,EntitySpawnReason.COMMAND);cow.setPos(MIN.getX()+2.5,MIN.getY()+1,MIN.getZ()+2.5);cow.setNoAi(true);cow.setCustomName(Component.literal("Restart cow"));cow.setHealth(9);l.addFreshEntity(cow);
        later(server,20,()->{try {
        var move=RoomRelocationSpike.capture(l,MIN,MAX,DELTA);if(move.bodies.isEmpty())throw new IllegalStateException("Animal not loaded before move");NbtIo.write(move.save(),JOURNAL);
        try(var file=java.nio.channels.FileChannel.open(JOURNAL,StandardOpenOption.WRITE)){file.force(true);}
        try{move.apply(l,2);}catch(RoomRelocationSpike.InterruptedMove expected){}
        server.saveEverything(false,true,true);
        PortablePocketCabin.LOGGER.info("RELOCATION_PARTIAL_WORLD_SAVED; ABRUPT_EXIT");
        PortablePocketCabin.LOGGER.info("DEDICATED_SERVER_STARTUP_TEST_PASSED");
        Runtime.getRuntime().halt(0);
        }catch(Exception e){PortablePocketCabin.LOGGER.error("RELOCATION_RESTART_SPIKE_FAILED",e);server.halt(false);}});
    }
    static void verify(MinecraftServer server,ServerLevel l)throws Exception {
        var move=RoomRelocationSpike.load(NbtIo.read(JOURNAL));
        // Entity storage is asynchronous on chunk load; wait for the original animal before replay.
        later(server,40,()->{
            try{
                move.apply(l,0);move.apply(l,0);
                for(var c:move.cells){BlockPos dest=c.pos().offset(move.delta);if(!l.getBlockState(dest).equals(c.state()))throw new IllegalStateException("Changed state after process restart");
                    if(c.data()!=null){CompoundTag expected=c.data().copy();expected.putInt("x",dest.getX());expected.putInt("y",dest.getY());expected.putInt("z",dest.getZ());if(!l.getBlockEntity(dest).saveWithFullMetadata(l.registryAccess()).equals(expected))throw new IllegalStateException("Changed inventory/progress after restart");}}
                for(var b:move.bodies){var e=l.getEntityInAnyDimension(b.id());if(e==null||Math.abs(e.getX()-b.x())>0.01)throw new IllegalStateException("Original entity did not recover");}
                PortablePocketCabin.LOGGER.info("RELOCATION_ABRUPT_RESTART_RECOVERY_PASSED");PortablePocketCabin.LOGGER.info("DEDICATED_SERVER_RESTART_TEST_PASSED");
            }catch(Exception e){PortablePocketCabin.LOGGER.error("RELOCATION_RESTART_SPIKE_FAILED",e);}finally{server.halt(false);}
        });
    }
}
