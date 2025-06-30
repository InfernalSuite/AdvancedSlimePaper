package com.infernalsuite.asp.level;

import ca.spottedleaf.concurrentutil.util.Priority;
import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import ca.spottedleaf.moonrise.patches.chunk_system.level.poi.PoiChunk;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.ChunkLoadTask;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.GenericDataLoadTask;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.infernalsuite.asp.level.moonrise.SlimeEntityDataLoader;
import com.infernalsuite.asp.level.moonrise.SlimePoiDataLoader;
import com.infernalsuite.asp.serialization.slime.SlimeSerializer;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spigotmc.AsyncCatcher;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;

public class SlimeLevelInstance extends ServerLevel {


    public static LevelStorageSource CUSTOM_LEVEL_STORAGE;

    static {
        try {
            Path path = Files.createTempDirectory("swm-" + UUID.randomUUID().toString().substring(0, 5)).toAbsolutePath();
            DirectoryValidator directoryvalidator = LevelStorageSource.parseValidator(path.resolve("allowed_symlinks.txt"));
            CUSTOM_LEVEL_STORAGE = new LevelStorageSource(path, path, directoryvalidator, DataFixers.getDataFixer());

            FileUtils.forceDeleteOnExit(path.toFile());

        } catch (IOException ex) {
            throw new IllegalStateException("Couldn't create dummy file directory.", ex);
        }
    }

    private static final ExecutorService WORLD_SAVER_SERVICE = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
            .setNameFormat("SWM Pool Thread #%1$d").build());

    private final Object saveLock = new Object();

    public SlimeLevelInstance(SlimeBootstrap slimeBootstrap, PrimaryLevelData primaryLevelData,
                              ResourceKey<net.minecraft.world.level.Level> worldKey,
                              ResourceKey<LevelStem> dimensionKey, LevelStem worldDimension,
                              org.bukkit.World.Environment environment) throws IOException {

        super(slimeBootstrap, MinecraftServer.getServer(), MinecraftServer.getServer().executor,
                CUSTOM_LEVEL_STORAGE.createAccess(slimeBootstrap.initial().getName() + UUID.randomUUID(), dimensionKey),
                primaryLevelData, worldKey, worldDimension,
                MinecraftServer.getServer().progressListenerFactory.create(11), false, 0,
                Collections.emptyList(), true, null, environment, null, null);
        this.slimeInstance = new SlimeInMemoryWorld(slimeBootstrap, this);


        SlimePropertyMap propertyMap = slimeBootstrap.initial().getPropertyMap();

        this.serverLevelData.setDifficulty(Difficulty.valueOf(propertyMap.getValue(SlimeProperties.DIFFICULTY).toUpperCase()));
        this.serverLevelData.setSpawn(new BlockPos(
                        propertyMap.getValue(SlimeProperties.SPAWN_X),
                        propertyMap.getValue(SlimeProperties.SPAWN_Y),
                        propertyMap.getValue(SlimeProperties.SPAWN_Z)),
                propertyMap.getValue(SlimeProperties.SPAWN_YAW));
        super.chunkSource.setSpawnSettings(propertyMap.getValue(SlimeProperties.ALLOW_MONSTERS), propertyMap.getValue(SlimeProperties.ALLOW_ANIMALS));

        this.pvpMode = propertyMap.getValue(SlimeProperties.PVP);

        this.entityDataController = new SlimeEntityDataLoader(
                new ca.spottedleaf.moonrise.patches.chunk_system.io.datacontroller.EntityDataController.EntityRegionFileStorage(
                        new RegionStorageInfo(levelStorageAccess.getLevelId(), worldKey, "entities"),
                        levelStorageAccess.getDimensionPath(worldKey).resolve("entities"),
                        MinecraftServer.getServer().forceSynchronousWrites()
                ),
                this.chunkTaskScheduler,
                this
        );
        this.poiDataController = new SlimePoiDataLoader(this, this.chunkTaskScheduler);
    }

    @Override
    public @NotNull ChunkGenerator getGenerator(SlimeBootstrap slimeBootstrap) {
        String biomeStr = slimeBootstrap.initial().getPropertyMap().getValue(SlimeProperties.DEFAULT_BIOME);
        ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(biomeStr));
        Holder<Biome> defaultBiome = MinecraftServer.getServer().registryAccess().lookupOrThrow(Registries.BIOME).get(biomeKey).orElseThrow();
        return new SlimeLevelGenerator(defaultBiome, this);
    }

    @Override
    public void save(@Nullable ProgressListener progressUpdate, boolean forceSave, boolean savingDisabled, boolean close) {
        if (!savingDisabled) save();
    }

    public void unload(@NotNull LevelChunk chunk, ChunkEntitySlices slices, PoiChunk poiChunk) {
        slimeInstance.unload(chunk, slices, poiChunk);
    }

    @Override
    public void saveIncrementally(boolean doFull) {
        if(doFull) {
            //Avoid doing the internal save because it saves the level.dat into the temp folder. That causes pterodactyl users to have issues.
            save();
        }
    }

    public Future<?> save() {
        AsyncCatcher.catchOp("SWM world save");
        try {
            if (!this.slimeInstance.isReadOnly() && this.slimeInstance.getLoader() != null) {
                Bukkit.getPluginManager().callEvent(new WorldSaveEvent(getWorld()));

                //this.getChunkSource().save(forceSave);
                this.serverLevelData.setWorldBorder(this.getWorldBorder().createSettings());
                this.serverLevelData.setCustomBossEvents(MinecraftServer.getServer().getCustomBossEvents().save(MinecraftServer.getServer().registryAccess()));

                if (MinecraftServer.getServer().isStopped()) { // Make sure the world gets saved before stopping the server by running it from the main thread
                    saveInternal().get(); // Async wait for it to finish
                } else {
                    return this.saveInternal();
                }
            }
        } catch (Throwable e) {
            Bukkit.getLogger().log(Level.SEVERE, "There was a problem saving the SlimeLevelInstance " + serverLevelData.getLevelName(), e);
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    private Future<?> saveInternal() {
        synchronized (saveLock) { // Don't want to save the SlimeWorld from multiple threads simultaneously
            SlimeWorldInstance slimeWorld = this.slimeInstance;
            Bukkit.getLogger().log(Level.INFO, "Saving world " + this.slimeInstance.getName() + "...");
            long start = System.currentTimeMillis();

            SlimeWorld world = this.slimeInstance.getSerializableCopy();
            return WORLD_SAVER_SERVICE.submit(() -> {
                try {
                    byte[] serializedWorld = SlimeSerializer.serialize(world);
                    long saveStart = System.currentTimeMillis();
                    slimeWorld.getLoader().saveWorld(slimeWorld.getName(), serializedWorld);
                    Bukkit.getLogger().log(Level.INFO, "World " + slimeWorld.getName() + " serialized in " + (saveStart - start) + "ms and saved in " + (System.currentTimeMillis() - saveStart) + "ms.");
                } catch (Exception ex) {
                    Bukkit.getLogger().log(Level.SEVERE, "There was an issue saving world " + slimeWorld.getName() + " asynchronously.", ex);
                }
            });

        }
    }

    public SlimeWorldInstance getSlimeInstance() {
        return this.slimeInstance;
    }

    public ChunkDataLoadTask getLoadTask(ChunkLoadTask task, ChunkTaskScheduler scheduler, ServerLevel world, int chunkX, int chunkZ, Priority priority, Consumer<GenericDataLoadTask.TaskResult<ChunkAccess, Throwable>> onRun) {
        return new ChunkDataLoadTask(task, scheduler, world, chunkX, chunkZ, priority, onRun);
    }

    @Override
    public void setDefaultSpawnPos(BlockPos pos, float angle) {
        super.setDefaultSpawnPos(pos, angle);

        SlimePropertyMap propertyMap = this.slimeInstance.getPropertyMap();
        propertyMap.setValue(SlimeProperties.SPAWN_X, pos.getX());
        propertyMap.setValue(SlimeProperties.SPAWN_Y, pos.getY());
        propertyMap.setValue(SlimeProperties.SPAWN_Z, pos.getZ());
        propertyMap.setValue(SlimeProperties.SPAWN_YAW, angle);
    }

    public void deleteTempFiles() {
        WORLD_SAVER_SERVICE.execute(() -> {
            Path path = this.levelStorageAccess.levelDirectory.path();
            try {
                // We do this manually and not use the deleteLevel function as it would cause a level deleted message
                // to appear in the log which might be confusing for our users
                Files.walkFileTree(path, new SimpleFileVisitor<>() {
                    @Override
                    public @NotNull FileVisitResult visitFile(Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                        if (!file.equals(path)) {
                            Files.deleteIfExists(file);
                        }

                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public @NotNull FileVisitResult postVisitDirectory(Path dir, @javax.annotation.Nullable IOException exception) throws IOException {
                        if (exception != null) {
                            throw exception;
                        } else {
                            if (dir.equals(levelStorageAccess.levelDirectory.path())) {
                                Files.deleteIfExists(path);
                            }

                            Files.deleteIfExists(dir);
                            return FileVisitResult.CONTINUE;
                        }
                    }
                });
            } catch (IOException e) {
                Bukkit.getLogger().log(Level.WARNING, "Unable to delete temp level directory" , e);
            }
        });
    }
}
