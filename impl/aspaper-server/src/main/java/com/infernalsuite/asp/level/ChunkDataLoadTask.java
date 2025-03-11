package com.infernalsuite.asp.level;

import ca.spottedleaf.concurrentutil.executor.PrioritisedExecutor;
import ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.ChunkLoadTask;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.GenericDataLoadTask;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.Converter;
import net.minecraft.nbt.CompoundTag;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.slf4j.Logger;

import java.util.function.Consumer;

public final class ChunkDataLoadTask implements CommonLoadTask {

    private static final Logger LOGGER = LogUtils.getClassLogger();

    private final ChunkTaskScheduler scheduler;
    private final ServerLevel world;
    private final int chunkX;
    private final int chunkZ;
    private Consumer<GenericDataLoadTask.TaskResult<ChunkAccess, Throwable>> onRun;

    private PrioritisedExecutor.PrioritisedTask task;

    private final ChunkLoadTask chunkLoadTask;

    protected ChunkDataLoadTask(ChunkLoadTask chunkLoadTask, final ChunkTaskScheduler scheduler, final ServerLevel world, final int chunkX,
                                final int chunkZ, final Priority priority, final Consumer<GenericDataLoadTask.TaskResult<ChunkAccess, Throwable>> onRun) {
        this.chunkLoadTask = chunkLoadTask;
        this.scheduler = scheduler;
        this.world = world;
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.onRun = onRun;

        this.task = this.scheduler.createChunkTask(this.chunkX, this.chunkZ, () -> {
            try {
                SlimeChunk chunk = ((SlimeLevelInstance) this.world).slimeInstance.getChunk(this.chunkX, this.chunkZ);
                this.onRun.accept(new GenericDataLoadTask.TaskResult<>(runOnMain(chunk), null));
            } catch (final Exception e) {
                LOGGER.error("ERROR", e);
                this.onRun.accept(new GenericDataLoadTask.TaskResult<>(null, e));
            }
        }, priority);
    }

    private ChunkAccess getEmptyChunk() {
        LevelChunkTicks<Block> blockLevelChunkTicks = new LevelChunkTicks<>();
        LevelChunkTicks<Fluid> fluidLevelChunkTicks = new LevelChunkTicks<>();

        return new ImposterProtoChunk(new LevelChunk(this.world, new ChunkPos(this.chunkX, this.chunkZ), UpgradeData.EMPTY, blockLevelChunkTicks, fluidLevelChunkTicks,
                0L, null, chunk -> {}, null), true);
    }

    protected ChunkAccess runOnMain(final SlimeChunk data) {
        // have tasks to run (at this point, it's just the POI consistency checking)
        try {
            //                if (data.tasks != null) {
            //                    for (int i = 0, len = data.tasks.size(); i < len; i) {
            //                        data.tasks.poll().run();
            //                    }
            //                }

            LevelChunk chunk = this.world.slimeInstance.promote(chunkX, chunkZ, data);

            ImposterProtoChunk protoChunk = new ImposterProtoChunk(chunk, false);
//            if (data != null) {
//                data.getEntities().stream().map(flowTag -> (CompoundTag) Converter.convertTag(flowTag)).forEach(protoChunk::addEntity);
//            }

            return protoChunk;
        } catch (final Exception e) {
            LOGGER.error("Failed to parse main tasks for task {}, chunk data will be lost", this, e);
            return this.getEmptyChunk();
        }
    }

    @Override
    public Priority getPriority() {
        return this.task.getPriority();
    }

    @Override
    public void setPriority(Priority priority) {
        this.task.setPriority(priority);
    }

    @Override
    public void raisePriority(Priority priority) {
        this.task.raisePriority(priority);
    }

    @Override
    public void lowerPriority(Priority priority) {
        this.task.lowerPriority(priority);
    }

    @Override
    public boolean cancel() {
        return this.task.cancel();
    }

    public boolean schedule(boolean schedule) {
        this.scheduler.scheduleChunkTask(chunkX, chunkZ, this.task::execute);
        return true;
    }
}
