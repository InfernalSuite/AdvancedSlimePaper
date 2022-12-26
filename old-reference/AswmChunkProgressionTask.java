package com.grinderwolf.swm.nms.v1192;

import ca.spottedleaf.concurrentutil.executor.standard.PrioritisedExecutor;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.nms.NmsUtil;
import com.mojang.datafixers.util.Either;
import com.mojang.logging.LogUtils;
import io.papermc.paper.chunk.system.poi.PoiChunk;
import io.papermc.paper.chunk.system.scheduling.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * This class is a identical copy of {@link ChunkLoadTask}.
 * We need to keep the package like this inorder to allow for package private accessing.
 * Look at the TASKS for the modified behavior.
 */
public final class AswmChunkProgressionTask extends ChunkProgressionTask {

    private static final Logger LOGGER = LogUtils.getClassLogger();

    private final NewChunkHolder chunkHolder;
    private final ChunkDataLoadTask loadTask;

    private boolean cancelled;
    private NewChunkHolder.GenericDataLoadTaskCallback entityLoadTask;
    private NewChunkHolder.GenericDataLoadTaskCallback poiLoadTask;

    private ReentrantLock schedulingLock;

    protected AswmChunkProgressionTask(final ChunkTaskScheduler scheduler, final ServerLevel world, final int chunkX, final int chunkZ,
                                       final NewChunkHolder chunkHolder, final PrioritisedExecutor.Priority priority) {
        super(scheduler, world, chunkX, chunkZ);
        this.chunkHolder = chunkHolder;
        this.loadTask = new ChunkDataLoadTask(scheduler, world, chunkX, chunkZ, priority, (result) -> {
            AswmChunkProgressionTask.this.complete(result == null ? null : result.left().orElse(null), result == null ? null : result.right().orElse(null));
        });


        try {
            Field field = ChunkTaskScheduler.class.getDeclaredField("schedulingLock");
            field.setAccessible(true);

            schedulingLock = (ReentrantLock) field.get(this.scheduler);
        } catch (Throwable e) {
            throw new AssertionError(e);
        }
    }

    public static Object create(Object scheduler, Object world, int chunkX, int chunkZ, Object chunkHolder, Object priority, Object status) {
        ChunkStatus chunkStatus = (ChunkStatus) status;
        if (chunkStatus != ChunkStatus.EMPTY) {
            return null;
        }

        return new AswmChunkProgressionTask((ChunkTaskScheduler) scheduler, (ServerLevel) world, chunkX, chunkZ, (NewChunkHolder) chunkHolder, (PrioritisedExecutor.Priority) priority);
    }

    @Override
    public ChunkStatus getTargetStatus() {
        return ChunkStatus.EMPTY;
    }

    private boolean scheduled;

    @Override
    public boolean isScheduled() {
        return this.scheduled;
    }

    @Override
    public void schedule() {
        final NewChunkHolder.GenericDataLoadTaskCallback entityLoadTask;
        final NewChunkHolder.GenericDataLoadTaskCallback poiLoadTask;

        final AtomicInteger count = new AtomicInteger();
        final Consumer<GenericDataLoadTask.TaskResult<?, ?>> scheduleLoadTask = (final GenericDataLoadTask.TaskResult<?, ?> result) -> {
            if (count.decrementAndGet() == 0) {
                AswmChunkProgressionTask.this.loadTask.schedule(false);
            }
        };

        // NOTE: it is IMPOSSIBLE for getOrLoadEntityData/getOrLoadPoiData to complete synchronously, because
        // they must schedule a task to off main or to on main to complete
        this.schedulingLock.lock();
        try {
            if (this.scheduled) {
                throw new IllegalStateException("schedule() called twice");
            }
            this.scheduled = true;
            if (this.cancelled) {
                return;
            }
            if (!this.chunkHolder.isEntityChunkNBTLoaded()) {
                entityLoadTask = this.chunkHolder.getOrLoadEntityData((Consumer) scheduleLoadTask);
                count.setPlain(count.getPlain() + 1);
            } else {
                entityLoadTask = null;
            }

            if (!this.chunkHolder.isPoiChunkLoaded()) {
                poiLoadTask = this.chunkHolder.getOrLoadPoiData((Consumer) scheduleLoadTask);
                count.setPlain(count.getPlain() + 1);
            } else {
                poiLoadTask = null;
            }

            this.entityLoadTask = entityLoadTask;
            this.poiLoadTask = poiLoadTask;
        } finally {
            this.schedulingLock.unlock();
        }

        if (entityLoadTask != null) {
            entityLoadTask.schedule();
        }

        if (poiLoadTask != null) {
            poiLoadTask.schedule();
        }

        if (entityLoadTask == null && poiLoadTask == null) {
            // no need to wait on those, we can schedule now
            this.loadTask.schedule(false);
        }
    }

    @Override
    public void cancel() {
        // must be before load task access, so we can synchronise with the writes to the fields
        this.schedulingLock.lock();
        try {
            this.cancelled = true;
        } finally {
            this.schedulingLock.unlock();
        }

        /*
        Note: The entityLoadTask/poiLoadTask do not complete when cancelled,
        but this is fine because if they are successfully cancelled then
        we will successfully cancel the load task, which will complete when cancelled
        */

        if (this.entityLoadTask != null) {
            this.entityLoadTask.cancel();
        }
        if (this.poiLoadTask != null) {
            this.poiLoadTask.cancel();
        }
        this.loadTask.cancel();
    }

    @Override
    public PrioritisedExecutor.Priority getPriority() {
        return this.loadTask.getPriority();
    }

    @Override
    public void lowerPriority(final PrioritisedExecutor.Priority priority) {
        final ChunkLoadTask.EntityDataLoadTask entityLoad = this.chunkHolder.getEntityDataLoadTask();
        if (entityLoad != null) {
            entityLoad.lowerPriority(priority);
        }

        final ChunkLoadTask.PoiDataLoadTask poiLoad = this.chunkHolder.getPoiDataLoadTask();

        if (poiLoad != null) {
            poiLoad.lowerPriority(priority);
        }

        this.loadTask.lowerPriority(priority);
    }

    @Override
    public void setPriority(final PrioritisedExecutor.Priority priority) {
        final ChunkLoadTask.EntityDataLoadTask entityLoad = this.chunkHolder.getEntityDataLoadTask();
        if (entityLoad != null) {
            entityLoad.setPriority(priority);
        }

        final ChunkLoadTask.PoiDataLoadTask poiLoad = this.chunkHolder.getPoiDataLoadTask();

        if (poiLoad != null) {
            poiLoad.setPriority(priority);
        }

        this.loadTask.setPriority(priority);
    }

    @Override
    public void raisePriority(final PrioritisedExecutor.Priority priority) {
        final ChunkLoadTask.EntityDataLoadTask entityLoad = this.chunkHolder.getEntityDataLoadTask();
        if (entityLoad != null) {
            entityLoad.raisePriority(priority);
        }

        final ChunkLoadTask.PoiDataLoadTask poiLoad = this.chunkHolder.getPoiDataLoadTask();

        if (poiLoad != null) {
            poiLoad.raisePriority(priority);
        }

        this.loadTask.raisePriority(priority);
    }

    public final class ChunkDataLoadTask implements PrioritisedExecutor.PrioritisedTask {

        private final ChunkTaskScheduler scheduler;
        private final ServerLevel world;
        private final int chunkX;
        private final int chunkZ;
        private Consumer<Either<ChunkAccess, Throwable>> onRun;

        private PrioritisedExecutor.PrioritisedTask task;

        protected ChunkDataLoadTask(final ChunkTaskScheduler scheduler, final ServerLevel world, final int chunkX,
                                    final int chunkZ, final PrioritisedExecutor.Priority priority, final Consumer<Either<ChunkAccess, Throwable>> onRun) {
            this.scheduler = scheduler;
            this.world = world;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.onRun = onRun;

            this.task = this.scheduler.createChunkTask(this.chunkX, this.chunkZ, () -> {
                try {
                    SlimeChunk chunk = ((CustomWorldServer) this.world).getSlimeWorld().getChunk(this.chunkX, this.chunkZ);
                    this.onRun.accept(Either.left(runOnMain(chunk)));
                } catch (Throwable e) {
                    LOGGER.error("ERROR", e);
                    this.onRun.accept(Either.right(e));
                }
            }, priority);
        }

        private ChunkAccess getEmptyChunk() {
            LevelChunkTicks<Block> blockLevelChunkTicks = new LevelChunkTicks<>();
            LevelChunkTicks<Fluid> fluidLevelChunkTicks = new LevelChunkTicks<>();

            return new ImposterProtoChunk(new LevelChunk(this.world, new ChunkPos(this.chunkX, this.chunkZ), UpgradeData.EMPTY, blockLevelChunkTicks, fluidLevelChunkTicks,
                    0L, null, null, null), true);
        }

        protected ChunkAccess runOnMain(final SlimeChunk data) {
            final PoiChunk poiChunk = AswmChunkProgressionTask.this.chunkHolder.getPoiChunk();
            if (poiChunk == null) {
                LOGGER.error("Expected poi chunk to be loaded with chunk for task " + this.toString());
            } else {
                poiChunk.load();
            }

            // have tasks to run (at this point, it's just the POI consistency checking)
            try {
//                if (data.tasks != null) {
//                    for (int i = 0, len = data.tasks.size(); i < len; ++i) {
//                        data.tasks.poll().run();
//                    }
//                }

                SlimeChunk slimeChunk = data;
                v1192SlimeWorld slimeWorld = ((CustomWorldServer) this.world).getSlimeWorld();
                LevelChunk chunk;

                if (slimeChunk == null) {
                    ChunkPos pos = new ChunkPos(this.chunkX, this.chunkZ);
                    LevelChunkTicks<Block> blockLevelChunkTicks = new LevelChunkTicks<>();
                    LevelChunkTicks<Fluid> fluidLevelChunkTicks = new LevelChunkTicks<>();

                    chunk = new LevelChunk(this.world, pos, UpgradeData.EMPTY, blockLevelChunkTicks, fluidLevelChunkTicks,
                            0L, null, null, null);

                    slimeWorld.updateChunk(new NMSSlimeChunk(null, chunk));
                } else if (slimeChunk instanceof NMSSlimeChunk nmsSlimeChunk) {
                    // Recreate chunk, can't reuse the chunk holders
                    LevelChunk backing = nmsSlimeChunk.getChunk();
                    chunk = new LevelChunk(backing.level, backing.getPos(), backing.getUpgradeData(), (LevelChunkTicks<Block>) backing.getBlockTicks(), (LevelChunkTicks<Fluid>) backing.getFluidTicks(), backing.getInhabitedTime(), backing.getSections(), null, null);
                    for (BlockEntity block : backing.getBlockEntities().values()) {
                        chunk.addAndRegisterBlockEntity(block);
                    }
                } else {
                    AtomicReference<NMSSlimeChunk> jank = new AtomicReference<>();
                    chunk = ((CustomWorldServer) this.world).convertChunk(slimeChunk, () -> {
                        jank.get().dirtySlime();
                    });

                    NMSSlimeChunk nmsSlimeChunk = new NMSSlimeChunk(slimeChunk, chunk);
                    jank.set(nmsSlimeChunk);

                    slimeWorld.updateChunk(nmsSlimeChunk);
                }


                List<com.flowpowered.nbt.CompoundTag> entities = slimeWorld.getEntities().get(NmsUtil.asLong(this.chunkX, this.chunkZ));
                if (entities != null) {
                    this.world.getEntityLookup().addLegacyChunkEntities(new ArrayList<>(
                            EntityType.loadEntitiesRecursive(entities
                                            .stream()
                                            .map((tag) -> (net.minecraft.nbt.CompoundTag) Converter.convertTag(tag))
                                            .collect(Collectors.toList()), this.world)
                                    .toList()
                    ));
                }

                return new ImposterProtoChunk(chunk, false);
            } catch (final ThreadDeath death) {
                throw death;
            } catch (final Throwable thr2) {
                LOGGER.error("Failed to parse main tasks for task " + this.toString() + ", chunk data will be lost", thr2);
                return this.getEmptyChunk();
            }
        }

        @Override
        public PrioritisedExecutor.Priority getPriority() {
            return this.task.getPriority();
        }

        @Override
        public boolean setPriority(PrioritisedExecutor.Priority priority) {
            return this.task.setPriority(priority);
        }

        @Override
        public boolean raisePriority(PrioritisedExecutor.Priority priority) {
            return this.task.raisePriority(priority);
        }

        @Override
        public boolean lowerPriority(PrioritisedExecutor.Priority priority) {
            return this.task.lowerPriority(priority);
        }

        @Override
        public boolean queue() {
            return this.task.queue();
        }

        @Override
        public boolean cancel() {
            return this.task.cancel();
        }

        @Override
        public boolean execute() {
            return this.task.execute();
        }

        public void schedule(boolean schedule) {
            this.scheduler.scheduleChunkTask(chunkX, chunkZ, this.task::execute);
        }
    }

}
