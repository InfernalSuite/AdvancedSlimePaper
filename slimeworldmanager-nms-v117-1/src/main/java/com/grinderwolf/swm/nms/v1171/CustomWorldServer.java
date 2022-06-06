package com.grinderwolf.swm.nms.v1171;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.LongArrayTag;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.NmsUtil;
import com.mojang.serialization.Codec;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ServerLevelData;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftHumanEntity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.generator.BiomeProvider;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CustomWorldServer extends ServerLevel {

    private static final ExecutorService WORLD_SAVER_SERVICE = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
            .setNameFormat("SWM Pool Thread #%1$d").build());
    private static final TicketType<Unit> SWM_TICKET = TicketType.create("swm-chunk", (a, b) -> 0);

    @Getter
    private final v1171SlimeWorld slimeWorld;
    private final Object saveLock = new Object();
    private final BiomeSource defaultBiomeSource;

    @Getter
    @Setter
    private boolean ready = false;

    public CustomWorldServer(v1171SlimeWorld world, ServerLevelData worldData,
                             ResourceKey<net.minecraft.world.level.Level> worldKey, ResourceKey<LevelStem> dimensionKey,
                             DimensionType dimensionManager, ChunkGenerator chunkGenerator,
                             org.bukkit.World.Environment environment, org.bukkit.generator.ChunkGenerator gen, BiomeProvider biomeProvider) throws IOException {
        super(MinecraftServer.getServer(), MinecraftServer.getServer().executor,
                v1171SlimeNMS.CUSTOM_LEVEL_STORAGE.createAccess(world.getName() + UUID.randomUUID(), dimensionKey),
                worldData, worldKey, dimensionManager, MinecraftServer.getServer().progressListenerFactory.create(11),
                chunkGenerator, false, 0, new ArrayList<>(), true, environment, gen, biomeProvider);

        this.slimeWorld = world;

        SlimePropertyMap propertyMap = world.getPropertyMap();

        this.serverLevelData.setDifficulty(Difficulty.valueOf(propertyMap.getValue(SlimeProperties.DIFFICULTY).toUpperCase()));
        this.serverLevelData.setSpawn(new BlockPos(propertyMap.getValue(SlimeProperties.SPAWN_X), propertyMap.getValue(SlimeProperties.SPAWN_Y), propertyMap.getValue(SlimeProperties.SPAWN_Z)), 0);
        super.setSpawnSettings(propertyMap.getValue(SlimeProperties.ALLOW_MONSTERS), propertyMap.getValue(SlimeProperties.ALLOW_ANIMALS));

        this.pvpMode = propertyMap.getValue(SlimeProperties.PVP);
        {
            String biomeStr = slimeWorld.getPropertyMap().getValue(SlimeProperties.DEFAULT_BIOME);
            ResourceKey<Biome> biomeKey = ResourceKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(biomeStr));
            Biome defaultBiome = MinecraftServer.getServer().registryAccess().ownedRegistryOrThrow(Registry.BIOME_REGISTRY).get(biomeKey);
            this.defaultBiomeSource = new BiomeSource(Collections.emptyList()) {
                @Override
                public Biome getNoiseBiome(int biomeX, int biomeY, int biomeZ) {
                    return defaultBiome;
                }

                @Override
                protected Codec<? extends BiomeSource> codec() {
                    return null;
                }

                @Override
                public BiomeSource withSeed(long seed) {
                    return this;
                }
            };
        }

    }

    public static CompletableFuture<Integer> relight(net.minecraft.world.level.Level world, Collection<? extends LevelChunk> chunks) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        ServerLevel level = world.getMinecraftWorld();

        Set<ChunkPos> chunkPos = chunks.stream()
                .map(LevelChunk::getPos)
                .collect(Collectors.toSet());

        level.chunkSource.getLightEngine().relight(chunkPos, pos -> {
        }, future::complete);

        return future;
    }

    @Override
    public void save(@Nullable ProgressListener progressUpdate, boolean forceSave, boolean flag1) {
        if (!slimeWorld.isReadOnly() && !flag1) {
            Bukkit.getPluginManager().callEvent(new WorldSaveEvent(getWorld()));

            this.chunkSource.save(forceSave);
            this.serverLevelData.setWorldBorder(this.getWorldBorder().createSettings());
            this.serverLevelData.setCustomBossEvents(MinecraftServer.getServer().getCustomBossEvents().save());

            // Update level data
            net.minecraft.nbt.CompoundTag compound = new net.minecraft.nbt.CompoundTag();
            net.minecraft.nbt.CompoundTag nbtTagCompound = this.serverLevelData.createTag(MinecraftServer.getServer().registryAccess(), compound);
            slimeWorld.getExtraData().getValue().put(Converter.convertTag("LevelData", nbtTagCompound));

            if (MinecraftServer.getServer().isStopped()) { // Make sure the world gets saved before stopping the server by running it from the main thread
                save();

                // Have to manually unlock the world as well
                try {
                    slimeWorld.getLoader().unlockWorld(slimeWorld.getName());
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (UnknownWorldException ignored) {

                }
            } else {
                WORLD_SAVER_SERVICE.execute(this::save);
            }
        }
    }

    private void save() {
        synchronized (saveLock) { // Don't want to save the SlimeWorld from multiple threads simultaneously
            try {
                Bukkit.getLogger().log(Level.INFO, "Saving world " + slimeWorld.getName() + "...");
                long start = System.currentTimeMillis();
                CompletableFuture<byte[]> serializedWorld = slimeWorld.serialize();
                slimeWorld.getLoader().saveWorld(slimeWorld.getName(), serializedWorld.join(), false);
                Bukkit.getLogger().log(Level.INFO, "World " + slimeWorld.getName() + " saved in " + (System.currentTimeMillis() - start) + "ms.");
            } catch (IOException | IllegalStateException ex) {
                ex.printStackTrace();
            }
        }
    }


    ImposterProtoChunk getImposterChunk(int x, int z) {
        SlimeChunk slimeChunk = slimeWorld.getChunk(x, z);
        LevelChunk chunk;

        if (slimeChunk instanceof NMSSlimeChunk) {
            chunk = ((NMSSlimeChunk) slimeChunk).getChunk();
        } else {
            if (slimeChunk == null) {
                ChunkPos pos = new ChunkPos(x, z);

                // Biomes
                // Use the default biome source to automatically populate the map with the default biome.
                ChunkBiomeContainer biomeStorage = new ChunkBiomeContainer(MinecraftServer.getServer().registryHolder.registryOrThrow(Registry.BIOME_REGISTRY), this, pos, defaultBiomeSource);

                // Tick lists
                ProtoTickList<Block> blockTickList = new ProtoTickList<Block>((block) ->
                        block == null || block.defaultBlockState().isAir(), pos, this);
                ProtoTickList<Fluid> fluidTickList = new ProtoTickList<Fluid>((type) ->
                        type == null || type == Fluids.EMPTY, pos, this);

                chunk = new LevelChunk(this, pos, biomeStorage, UpgradeData.EMPTY, blockTickList, fluidTickList,
                        0L, null, null);

                // Height Maps
//                HeightMap.a(chunk, ChunkStatus.FULL.h());
            } else {
                chunk = createChunk(slimeChunk);
            }

            slimeWorld.updateChunk(new NMSSlimeChunk(chunk));
        }

        return new ImposterProtoChunk(chunk);
    }

    private boolean isSectionEmptyAsync(LevelChunkSection section) {
        AtomicBoolean empty = new AtomicBoolean(true);
        section.states.forEachLocation((state, location) -> {
            if (!empty.get()) return;

            if (!state.isAir() || state.getFluidState().isEmpty()) {
                empty.set(false);
            }
        });

        return empty.get();
    }

    private LevelChunk createChunk(SlimeChunk chunk) {
        int x = chunk.getX();
        int z = chunk.getZ();

        ChunkPos pos = new ChunkPos(x, z);

        // Biomes
        int[] biomeIntArray = chunk.getBiomes();

        ChunkBiomeContainer biomeStorage = new ChunkBiomeContainer(MinecraftServer.getServer().registryHolder.registryOrThrow(Registry.BIOME_REGISTRY), this, pos,
                chunkSource.generator.getBiomeSource(), biomeIntArray);

        // Tick lists
        ProtoTickList<Block> blockTickList = new ProtoTickList<Block>((block) ->
                block == null || block.defaultBlockState().isAir(), pos, this);
        ProtoTickList<Fluid> fluidTickList = new ProtoTickList<Fluid>((type) ->
                type == null || type == Fluids.EMPTY, pos, this);

        // Chunk sections
        LevelChunkSection[] sections = new LevelChunkSection[16];
//        LightEngine lightEngine = getChunkProvider().getLightEngine();
//
//        lightEngine.b(pos, true);

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            SlimeChunkSection slimeSection = chunk.getSections()[sectionId];

            if (slimeSection != null) {
                LevelChunkSection section = new LevelChunkSection(sectionId << 4);

                section.states.read((ListTag) Converter.convertTag(slimeSection.getPalette()), slimeSection.getBlockStates());

//                if (slimeSection.getBlockLight() != null) {
//                    lightEngine.a(EnumSkyBlock.b, SectionPosition.a(pos, sectionId), Converter.convertArray(slimeSection.getBlockLight()), true);
//                }
//
//                if (slimeSection.getSkyLight() != null) {
//                    lightEngine.a(EnumSkyBlock.a, SectionPosition.a(pos, sectionId), Converter.convertArray(slimeSection.getSkyLight()), true);
//                }

                if (!isSectionEmptyAsync(section)) {
                    section.recalcBlockCounts();
                }
                sections[sectionId] = section;
            }
        }

        // Keep the chunk loaded at level 33 to avoid light glitches
        // Such a high level will let the server not tick the chunk,
        // but at the same time it won't be completely unloaded from memory
//        getChunkProvider().addTicket(SWM_TICKET, pos, 33, Unit.INSTANCE);

        Consumer<LevelChunk> loadEntities = (nmsChunk) -> {
            relight(this, List.of(nmsChunk));

            // Load tile entities
//            System.out.println("Loading tile entities for chunk (" + pos.x + ", " + pos.z + ") on world " + slimeWorld.getName());
            List<CompoundTag> tileEntities = chunk.getTileEntities();
            int loadedEntities = 0;

            if (tileEntities != null) {
                for (CompoundTag tag : tileEntities) {
                    Optional<String> type = tag.getStringValue("id");

                    // Sometimes null tile entities are saved
                    if (type.isPresent()) {
                        BlockPos blockPosition = new BlockPos(tag.getIntValue("x").get(), tag.getIntValue("y").get(), tag.getIntValue("z").get());
                        BlockState blockData = nmsChunk.getBlockState(blockPosition);
                        BlockEntity entity = BlockEntity.loadStatic(blockPosition, blockData, (net.minecraft.nbt.CompoundTag) Converter.convertTag(tag));

                        if (entity != null) {
                            nmsChunk.setBlockEntity(entity);
                            loadedEntities++;
                        }
                    }
                }
            }
        };

        LevelChunk nmsChunk = new LevelChunk(this, pos, biomeStorage,
                UpgradeData.EMPTY,
                blockTickList, fluidTickList, 0L, sections, loadEntities);

        // Height Maps
        EnumSet<Heightmap.Types> heightMapTypes = nmsChunk.getStatus().heightmapsAfter();
        CompoundMap heightMaps = chunk.getHeightMaps().getValue();
        EnumSet<Heightmap.Types> unsetHeightMaps = EnumSet.noneOf(Heightmap.Types.class);

        for (Heightmap.Types type : heightMapTypes) {
            String name = type.getSerializedName();

            if (heightMaps.containsKey(name)) {
                LongArrayTag heightMap = (LongArrayTag) heightMaps.get(name);
                nmsChunk.setHeightmap(type, heightMap.getValue());
            } else {
                unsetHeightMaps.add(type);
            }
        }

        // Don't try to populate heightmaps if there are none.
        // Does a crazy amount of block lookups
        if (!unsetHeightMaps.isEmpty()) {
            Heightmap.primeHeightmaps(nmsChunk, unsetHeightMaps);
        }

        return nmsChunk;
    }

    void saveChunk(LevelChunk chunk) {
        SlimeChunk slimeChunk = slimeWorld.getChunk(chunk.getPos().x, chunk.getPos().z);

        // In case somehow the chunk object changes (might happen for some reason)
        if (slimeChunk instanceof NMSSlimeChunk) {
            ((NMSSlimeChunk) slimeChunk).setChunk(chunk);
        } else {
            slimeWorld.updateChunk(new NMSSlimeChunk(chunk));
        }
    }

    public CompletableFuture<ChunkEntities<Entity>> handleEntityLoad(EntityStorage storage, ChunkPos pos) {
        List<CompoundTag> entities = slimeWorld.getEntities().get(NmsUtil.asLong(pos.x, pos.z));
        if (entities == null) {
            entities = new ArrayList<>();
        }

        return CompletableFuture.completedFuture(new ChunkEntities<>(pos, new ArrayList<>(
                EntityType.loadEntitiesRecursive(entities
                                .stream()
                                .map((tag) -> (net.minecraft.nbt.CompoundTag) Converter.convertTag(tag))
                                .collect(Collectors.toList()), this)
                        .toList()
        )));


    }

    public void handleEntityUnLoad(EntityStorage storage, ChunkEntities<Entity> entities) {
        ChunkPos pos = entities.getPos();
        List<CompoundTag> entitiesSerialized = new ArrayList<>();

        entities.getEntities().forEach((entity) -> {
            net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
            if (entity.save(tag)) {
                entitiesSerialized.add((CompoundTag) Converter.convertTag("", tag));
            }

        });

        slimeWorld.getEntities().put(NmsUtil.asLong(pos.x, pos.z), entitiesSerialized);
    }

    @Override
    public void unload(LevelChunk chunk) {
        Iterator<BlockEntity> tileEntities = chunk.getBlockEntities().values().iterator();
        do {
            BlockEntity tileentity;
            do {
                if (!tileEntities.hasNext()) {
//                    chunk.C();
                    return;
                }
                tileentity = tileEntities.next();
            } while (!(tileentity instanceof Container));

            for (HumanEntity h : Lists.newArrayList(((Container) tileentity).getViewers())) {
                ((CraftHumanEntity) h).getHandle().closeUnloadedInventory(InventoryCloseEvent.Reason.UNLOADED);
            }
            ((Container) tileentity).getViewers().clear();
        } while (true);
    }
}
