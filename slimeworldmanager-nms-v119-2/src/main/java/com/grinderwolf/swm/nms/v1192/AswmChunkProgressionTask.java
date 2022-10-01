package com.grinderwolf.swm.nms.v1192;

import ca.spottedleaf.concurrentutil.collection.MultiThreadedQueue;
import ca.spottedleaf.concurrentutil.executor.standard.PrioritisedExecutor;
import ca.spottedleaf.concurrentutil.util.ConcurrentUtil;
import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.mojang.logging.LogUtils;
import io.papermc.paper.chunk.system.io.RegionFileIOThread;
import io.papermc.paper.chunk.system.poi.PoiChunk;
import io.papermc.paper.chunk.system.scheduling.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.slf4j.Logger;
import java.lang.invoke.VarHandle;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

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
        this.loadTask = new ChunkDataLoadTask(scheduler, world, chunkX, chunkZ, priority);
        this.loadTask.addCallback((final GenericDataLoadTask.TaskResult<ChunkAccess, Throwable> result) -> {
            AswmChunkProgressionTask.this.complete(result == null ? null : result.left(), result == null ? null : result.right());
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
                entityLoadTask = this.chunkHolder.getOrLoadEntityData((Consumer)scheduleLoadTask);
                count.setPlain(count.getPlain() + 1);
            } else {
                entityLoadTask = null;
            }

            if (!this.chunkHolder.isPoiChunkLoaded()) {
                poiLoadTask = this.chunkHolder.getOrLoadPoiData((Consumer)scheduleLoadTask);
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

        if (poiLoadTask !=  null) {
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

    protected static abstract class CallbackDataLoadTask<OnMain,FinalCompletion> extends GenericDataLoadTask<OnMain,FinalCompletion> {

        private TaskResult<FinalCompletion, Throwable> result;
        private final MultiThreadedQueue<Consumer<TaskResult<FinalCompletion, Throwable>>> waiters = new MultiThreadedQueue<>();

        protected volatile boolean completed;
        protected static final VarHandle COMPLETED_HANDLE = ConcurrentUtil.getVarHandle(CallbackDataLoadTask.class, "completed", boolean.class);

        protected CallbackDataLoadTask(final ChunkTaskScheduler scheduler, final ServerLevel world, final int chunkX,
                                       final int chunkZ, final RegionFileIOThread.RegionFileType type,
                                       final PrioritisedExecutor.Priority priority) {
            super(scheduler, world, chunkX, chunkZ, type, priority);
        }

        public void addCallback(final Consumer<TaskResult<FinalCompletion, Throwable>> consumer) {
            if (!this.waiters.add(consumer)) {
                try {
                    consumer.accept(this.result);
                } catch (final Throwable throwable) {
                    this.scheduler.unrecoverableChunkSystemFailure(this.chunkX, this.chunkZ, Map.of(
                            "Consumer", ChunkTaskScheduler.stringIfNull(consumer),
                            "Completed throwable", ChunkTaskScheduler.stringIfNull(this.result.right())
                    ), throwable);
                    if (throwable instanceof ThreadDeath) {
                        throw (ThreadDeath)throwable;
                    }
                }
            }
        }

        @Override
        protected void onComplete(final TaskResult<FinalCompletion, Throwable> result) {
            if ((boolean)COMPLETED_HANDLE.getAndSet((CallbackDataLoadTask)this, (boolean)true)) {
                throw new IllegalStateException("Already completed");
            }
            this.result = result;
            Consumer<TaskResult<FinalCompletion, Throwable>> consumer;
            while ((consumer = this.waiters.pollOrBlockAdds()) != null) {
                try {
                    consumer.accept(result);
                } catch (final Throwable throwable) {
                    this.scheduler.unrecoverableChunkSystemFailure(this.chunkX, this.chunkZ, Map.of(
                            "Consumer", ChunkTaskScheduler.stringIfNull(consumer),
                            "Completed throwable", ChunkTaskScheduler.stringIfNull(result.right())
                    ), throwable);
                    if (throwable instanceof ThreadDeath) {
                        throw (ThreadDeath)throwable;
                    }
                    return;
                }
            }
        }
    }

    public final class ChunkDataLoadTask extends CallbackDataLoadTask<SlimeChunk, ChunkAccess> {
        protected ChunkDataLoadTask(final ChunkTaskScheduler scheduler, final ServerLevel world, final int chunkX,
                                    final int chunkZ, final PrioritisedExecutor.Priority priority) {
            super(scheduler, world, chunkX, chunkZ, RegionFileIOThread.RegionFileType.CHUNK_DATA, priority);
        }

        @Override
        protected boolean hasOffMain() {
            return true;
        }

        @Override
        protected boolean hasOnMain() {
            return true;
        }

        @Override
        protected PrioritisedExecutor.PrioritisedTask createOffMain(final Runnable run, final PrioritisedExecutor.Priority priority) {
            return this.scheduler.loadExecutor.createTask(run, priority);
        }

        @Override
        protected PrioritisedExecutor.PrioritisedTask createOnMain(final Runnable run, final PrioritisedExecutor.Priority priority) {
            return this.scheduler.createChunkTask(this.chunkX, this.chunkZ, run, priority);
        }

        @Override
        protected TaskResult<ChunkAccess, Throwable> completeOnMainOffMain(final SlimeChunk data, final Throwable throwable) {
            if (data != null) {
                return null;
            }

            final PoiChunk poiChunk = AswmChunkProgressionTask.this.chunkHolder.getPoiChunk();
            if (poiChunk == null) {
                LOGGER.error("Expected poi chunk to be loaded with chunk for task " + this.toString());
            } else if (!poiChunk.isLoaded()) {
                // need to call poiChunk.load() on main
                return null;
            }

            return new TaskResult<>(this.getEmptyChunk(), null);
        }

        @Override
        protected TaskResult<SlimeChunk, Throwable> runOffMain(final CompoundTag data, final Throwable throwable) {
            if (throwable != null) {
                LOGGER.error("Failed to load chunk data for task: " + this.toString() + ", chunk data will be lost", throwable);
                return new TaskResult<>(null, null);
            }

            if (data == null) {
                return new TaskResult<>(null, null);
            }

            // need to convert data, and then deserialize it

            try {
                final ChunkPos chunkPos = new ChunkPos(this.chunkX, this.chunkZ);
                final ChunkMap chunkMap = this.world.getChunkSource().chunkMap;
                SlimeChunk chunk = ((CustomWorldServer) this.world).getSlimeWorld().getChunk(this.chunkX, this.chunkZ);

                // run converters
                // note: upgradeChunkTag copies the data already
//                final CompoundTag converted = chunkMap.upgradeChunkTag(
//                        this.world.getTypeKey(), chunkMap.overworldDataStorage, data, chunkMap.generator.getTypeNameForDataFixer(),
//                        chunkPos, this.world
//                );

                return new TaskResult<>(chunk, null);
            } catch (final ThreadDeath death) {
                throw death;
            } catch (final Throwable thr2) {
                LOGGER.error("Failed to parse chunk data for task: " + this.toString() + ", chunk data will be lost", thr2);
                return new TaskResult<>(null, thr2);
            }
        }

        private ChunkAccess getEmptyChunk() {
            LevelChunkTicks<Block> blockLevelChunkTicks = new LevelChunkTicks<>();
            LevelChunkTicks<Fluid> fluidLevelChunkTicks = new LevelChunkTicks<>();

            return new ImposterProtoChunk(new LevelChunk(this.world, new ChunkPos(this.chunkX, this.chunkZ), UpgradeData.EMPTY, blockLevelChunkTicks, fluidLevelChunkTicks,
                    0L, null, null, null), true);
        }

        @Override
        protected TaskResult<ChunkAccess, Throwable> runOnMain(final SlimeChunk data, final Throwable throwable) {
            final PoiChunk poiChunk = AswmChunkProgressionTask.this.chunkHolder.getPoiChunk();
            if (poiChunk == null) {
                LOGGER.error("Expected poi chunk to be loaded with chunk for task " + this.toString());
            } else {
                poiChunk.load();
            }

            if (data == null) {
                // throwable could be non-null, but the off-main task will print its exceptions - so we don't need to care,
                // it's handled already

                return new TaskResult<>(this.getEmptyChunk(), null);
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
                    ChunkPos pos = new ChunkPos(data.getX(), data.getZ());
                    LevelChunkTicks<Block> blockLevelChunkTicks = new LevelChunkTicks<>();
                    LevelChunkTicks<Fluid> fluidLevelChunkTicks = new LevelChunkTicks<>();

                    chunk = new LevelChunk(this.world, pos, UpgradeData.EMPTY, blockLevelChunkTicks, fluidLevelChunkTicks,
                            0L, null, null, null);

                    slimeWorld.updateChunk(new NMSSlimeChunk(null, chunk));
                } else if (slimeChunk instanceof NMSSlimeChunk) {
                    chunk = ((NMSSlimeChunk) slimeChunk).getChunk(); // This shouldn't happen anymore, unloading should cleanup the chunk
                } else {
                    AtomicReference<NMSSlimeChunk> jank = new AtomicReference<>();
                    chunk = ((CustomWorldServer) this.world).convertChunk(slimeChunk, () -> {
                        jank.get().dirtySlime();
                    });

                    NMSSlimeChunk nmsSlimeChunk = new NMSSlimeChunk(slimeChunk, chunk);
                    jank.set(nmsSlimeChunk);

                    slimeWorld.updateChunk(nmsSlimeChunk);
                }

                return new TaskResult<>(new ImposterProtoChunk(chunk, false), null);
            } catch (final ThreadDeath death) {
                throw death;
            } catch (final Throwable thr2) {
                LOGGER.error("Failed to parse main tasks for task " + this.toString() + ", chunk data will be lost", thr2);
                return new TaskResult<>(this.getEmptyChunk(), null);
            }
        }
    }

    public static final class PoiDataLoadTask extends CallbackDataLoadTask<PoiChunk, PoiChunk> {
        public PoiDataLoadTask(final ChunkTaskScheduler scheduler, final ServerLevel world, final int chunkX,
                               final int chunkZ, final PrioritisedExecutor.Priority priority) {
            super(scheduler, world, chunkX, chunkZ, RegionFileIOThread.RegionFileType.POI_DATA, priority);
        }

        @Override
        protected boolean hasOffMain() {
            return true;
        }

        @Override
        protected boolean hasOnMain() {
            return false;
        }

        @Override
        protected PrioritisedExecutor.PrioritisedTask createOffMain(final Runnable run, final PrioritisedExecutor.Priority priority) {
            return this.scheduler.loadExecutor.createTask(run, priority);
        }

        @Override
        protected PrioritisedExecutor.PrioritisedTask createOnMain(final Runnable run, final PrioritisedExecutor.Priority priority) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected TaskResult<PoiChunk, Throwable> completeOnMainOffMain(final PoiChunk data, final Throwable throwable) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected TaskResult<PoiChunk, Throwable> runOffMain(CompoundTag data, final Throwable throwable) {
            if (throwable != null) {
                LOGGER.error("Failed to load poi data for task: " + this.toString() + ", poi data will be lost", throwable);
                return new TaskResult<>(PoiChunk.empty(this.world, this.chunkX, this.chunkZ), null);
            }

            if (data == null || data.isEmpty()) {
                // nothing to do
                return new TaskResult<>(PoiChunk.empty(this.world, this.chunkX, this.chunkZ), null);
            }

            try {
                data = data.copy(); // coming from the I/O thread, so we need to copy
                // run converters
                final int dataVersion = !data.contains(SharedConstants.DATA_VERSION_TAG, 99) ? 1945 : data.getInt(SharedConstants.DATA_VERSION_TAG);
                final CompoundTag converted = MCDataConverter.convertTag(
                        MCTypeRegistry.POI_CHUNK, data, dataVersion, SharedConstants.getCurrentVersion().getWorldVersion()
                );

                // now we need to parse it
                return new TaskResult<>(PoiChunk.parse(this.world, this.chunkX, this.chunkZ, converted), null);
            } catch (final ThreadDeath death) {
                throw death;
            } catch (final Throwable thr2) {
                LOGGER.error("Failed to run parse poi data for task: " + this.toString() + ", poi data will be lost", thr2);
                return new TaskResult<>(PoiChunk.empty(this.world, this.chunkX, this.chunkZ), null);
            }
        }

        @Override
        protected TaskResult<PoiChunk, Throwable> runOnMain(final PoiChunk data, final Throwable throwable) {
            throw new UnsupportedOperationException();
        }
    }

    public static final class EntityDataLoadTask extends CallbackDataLoadTask<CompoundTag, CompoundTag> {

        public EntityDataLoadTask(final ChunkTaskScheduler scheduler, final ServerLevel world, final int chunkX,
                                  final int chunkZ, final PrioritisedExecutor.Priority priority) {
            super(scheduler, world, chunkX, chunkZ, RegionFileIOThread.RegionFileType.ENTITY_DATA, priority);
        }

        @Override
        protected boolean hasOffMain() {
            return true;
        }

        @Override
        protected boolean hasOnMain() {
            return false;
        }

        @Override
        protected PrioritisedExecutor.PrioritisedTask createOffMain(final Runnable run, final PrioritisedExecutor.Priority priority) {
            return this.scheduler.loadExecutor.createTask(run, priority);
        }

        @Override
        protected PrioritisedExecutor.PrioritisedTask createOnMain(final Runnable run, final PrioritisedExecutor.Priority priority) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected TaskResult<CompoundTag, Throwable> completeOnMainOffMain(final CompoundTag data, final Throwable throwable) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected TaskResult<CompoundTag, Throwable> runOffMain(final CompoundTag data, final Throwable throwable) {
            if (throwable != null) {
                LOGGER.error("Failed to load entity data for task: " + this.toString() + ", entity data will be lost", throwable);
                return new TaskResult<>(null, null);
            }

            if (data == null || data.isEmpty()) {
                // nothing to do
                return new TaskResult<>(null, null);
            }

            try {
                // note: data comes from the I/O thread, so we need to copy it
                return new TaskResult<>(EntityStorage.upgradeChunkTag(data.copy()), null);
            } catch (final ThreadDeath death) {
                throw death;
            } catch (final Throwable thr2) {
                LOGGER.error("Failed to run converters for entity data for task: " + this.toString() + ", entity data will be lost", thr2);
                return new TaskResult<>(null, thr2);
            }
        }

        @Override
        protected TaskResult<CompoundTag, Throwable> runOnMain(final CompoundTag data, final Throwable throwable) {
            throw new UnsupportedOperationException();
        }
    }
}
