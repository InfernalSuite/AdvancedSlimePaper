package com.infernalsuite.asp.level;

import ca.spottedleaf.concurrentutil.executor.standard.PrioritisedExecutor;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.ChunkLoadTask;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.task.GenericDataLoadTask;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.serialization.slime.SlimeSerializer;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.serialization.slime.SlimeSerializer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.Unit;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.world.WorldSaveEvent;
import org.jetbrains.annotations.Nullable;
import org.spigotmc.AsyncCatcher;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

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
    private static final TicketType<Unit> SWM_TICKET = TicketType.create("swm-chunk", (a, b) -> 0);

    private final Object saveLock = new Object();

    public SlimeLevelInstance(SlimeBootstrap slimeBootstrap, PrimaryLevelData primaryLevelData,
                              ResourceKey<net.minecraft.world.level.Level> worldKey,
                              ResourceKey<LevelStem> dimensionKey, LevelStem worldDimension,
                              org.bukkit.World.Environment environment) throws IOException {

        super(slimeBootstrap, MinecraftServer.getServer(), MinecraftServer.getServer().executor,
                CUSTOM_LEVEL_STORAGE.createAccess(slimeBootstrap.initial().getName() + UUID.randomUUID(), dimensionKey),
                primaryLevelData, worldKey, worldDimension,
                MinecraftServer.getServer().progressListenerFactory.create(11), false, null, 0,
                Collections.emptyList(), true, environment, null, null);
        this.slimeInstance = new SlimeInMemoryWorld(slimeBootstrap, this);


        SlimePropertyMap propertyMap = slimeBootstrap.initial().getPropertyMap();

        this.serverLevelData.setDifficulty(Difficulty.valueOf(propertyMap.getValue(SlimeProperties.DIFFICULTY).toUpperCase()));
        this.serverLevelData.setSpawn(new BlockPos(
                        propertyMap.getValue(SlimeProperties.SPAWN_X),
                        propertyMap.getValue(SlimeProperties.SPAWN_Y),
                        propertyMap.getValue(SlimeProperties.SPAWN_Z)),
                propertyMap.getValue(SlimeProperties.SPAWN_YAW));
        super.setSpawnSettings(propertyMap.getValue(SlimeProperties.ALLOW_MONSTERS), propertyMap.getValue(SlimeProperties.ALLOW_ANIMALS));

        this.pvpMode = propertyMap.getValue(SlimeProperties.PVP);
    }

    @Override
    public ChunkGenerator getGenerator(SlimeBootstrap slimeBootstrap) {
        String biomeStr = slimeBootstrap.initial().getPropertyMap().getValue(SlimeProperties.DEFAULT_BIOME);
        ResourceKey<Biome> biomeKey = ResourceKey.create(Registries.BIOME, ResourceLocation.parse(biomeStr));
        Holder<Biome> defaultBiome = MinecraftServer.getServer().registryAccess().registryOrThrow(Registries.BIOME).getHolder(biomeKey).orElseThrow();
        return new SlimeLevelGenerator(defaultBiome);
    }

    @Override
    public void save(@Nullable ProgressListener progressUpdate, boolean forceSave, boolean savingDisabled, boolean close) {
        if (!savingDisabled) save();
    }

    public Future<?> save() {
        AsyncCatcher.catchOp("SWM world save");
        try {
            if (!this.slimeInstance.isReadOnly() && this.slimeInstance.getLoader() != null) {
                Bukkit.getPluginManager().callEvent(new WorldSaveEvent(getWorld()));

                //this.getChunkSource().save(forceSave);
                this.serverLevelData.setWorldBorder(this.getWorldBorder().createSettings());
                this.serverLevelData.setCustomBossEvents(MinecraftServer.getServer().getCustomBossEvents().save(MinecraftServer.getServer().registryAccess()));

                // Update level data
                net.minecraft.nbt.CompoundTag compound = new net.minecraft.nbt.CompoundTag();
                net.minecraft.nbt.CompoundTag nbtTagCompound = this.serverLevelData.createTag(MinecraftServer.getServer().registryAccess(), compound);

                if (MinecraftServer.getServer().isStopped()) { // Make sure the world gets saved before stopping the server by running it from the main thread
                    saveInternal().get(); // Async wait for it to finish
                } else {
                    return this.saveInternal();
                    //WORLD_SAVER_SERVICE.execute(this::save);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            return CompletableFuture.failedFuture(e);
        }
        return CompletableFuture.completedFuture(null);
    }

    /*
    @Override
    public void saveIncrementally(boolean doFull) {
        if (doFull) {
            this.save(null, false, false);
        }
    }*/ // Most likely unused - kyngs

    private Future<?> saveInternal() {
        synchronized (saveLock) { // Don't want to save the SlimeWorld from multiple threads simultaneously
            SlimeWorldInstance slimeWorld = this.slimeInstance;
            Bukkit.getLogger().log(Level.INFO, "Saving world " + this.slimeInstance.getName() + "...");
            long start = System.currentTimeMillis();

            SlimeWorld world = this.slimeInstance.getForSerialization();
            return WORLD_SAVER_SERVICE.submit(() -> {
                try {
                    byte[] serializedWorld = SlimeSerializer.serialize(world);
                    long saveStart = System.currentTimeMillis();
                    slimeWorld.getLoader().saveWorld(slimeWorld.getName(), serializedWorld);
                    Bukkit.getLogger().log(Level.INFO, "World " + slimeWorld.getName() + " serialized in " + (saveStart - start) + "ms and saved in " + (System.currentTimeMillis() - saveStart) + "ms.");
                } catch (IOException | IllegalStateException ex) {
                    ex.printStackTrace();
                }
            });

        }
    }

    public SlimeWorldInstance getSlimeInstance() {
        return this.slimeInstance;
    }

    public ChunkDataLoadTask getLoadTask(ChunkLoadTask task, ChunkTaskScheduler scheduler, ServerLevel world, int chunkX, int chunkZ, PrioritisedExecutor.Priority priority, Consumer<GenericDataLoadTask.TaskResult<ChunkAccess, Throwable>> onRun) {
        return new ChunkDataLoadTask(task, scheduler, world, chunkX, chunkZ, priority, onRun);
    }

    /*
    public void loadEntities(int chunkX, int chunkZ) {
        SlimeChunk slimeChunk = this.slimeInstance.getChunk(chunkX, chunkZ);
        if (slimeChunk != null) {
            this.getEntityLookup().addLegacyChunkEntities(new ArrayList<>(
                    EntityType.loadEntitiesRecursive(slimeChunk.getEntities()
                                    .stream()
                                    .map((tag) -> (net.minecraft.nbt.CompoundTag) Converter.convertTag(tag))
                                    .collect(Collectors.toList()), this)
                            .toList()
            ), new ChunkPos(chunkX, chunkZ));
        }
    }*/ // Most likely unused - kyngs

    @Override
    public void setDefaultSpawnPos(BlockPos pos, float angle) {
        super.setDefaultSpawnPos(pos, angle);

        SlimePropertyMap propertyMap = this.slimeInstance.getPropertyMap();
        propertyMap.setValue(SlimeProperties.SPAWN_X, pos.getX());
        propertyMap.setValue(SlimeProperties.SPAWN_Y, pos.getY());
        propertyMap.setValue(SlimeProperties.SPAWN_Z, pos.getZ());
        propertyMap.setValue(SlimeProperties.SPAWN_YAW, angle);
    }

    //    @Override
    //    public void unload(LevelChunk chunk) {
    //        this.slimeInstance.unload(chunk);
    //        super.unload(chunk);
    //    }
}
