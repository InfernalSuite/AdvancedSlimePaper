package com.grinderwolf.swm.nms.v1191;

import ca.spottedleaf.starlight.common.light.SWMRNibbleArray;
import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.LongArrayTag;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.NmsUtil;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.nbt.NbtOps;
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
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.entity.ChunkEntities;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.PrimaryLevelData;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_19_R1.entity.CraftHumanEntity;
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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class CustomWorldServer extends ServerLevel {

    private static final ExecutorService WORLD_SAVER_SERVICE = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
            .setNameFormat("SWM Pool Thread #%1$d").build());
    private static final TicketType<Unit> SWM_TICKET = TicketType.create("swm-chunk", (a, b) -> 0);

    @Getter
    private final v1191SlimeWorld slimeWorld;
    private final Object saveLock = new Object();
    private final BiomeSource defaultBiomeSource;

    @Getter
    @Setter
    private boolean ready = false;

    public CustomWorldServer(v1191SlimeWorld world, PrimaryLevelData primaryLevelData,
                             ResourceKey<net.minecraft.world.level.Level> worldKey,
                             ResourceKey<LevelStem> dimensionKey, LevelStem worldDimension,
                             org.bukkit.World.Environment environment, org.bukkit.generator.ChunkGenerator gen,
                             BiomeProvider biomeProvider) throws IOException {

        super(MinecraftServer.getServer(), MinecraftServer.getServer().executor,
                v1191SlimeNMS.CUSTOM_LEVEL_STORAGE.createAccess(world.getName() + UUID.randomUUID(),
                dimensionKey), primaryLevelData, worldKey, worldDimension,
                MinecraftServer.getServer().progressListenerFactory.create(11), false, 0,
                Collections.emptyList(), true, environment, gen, biomeProvider);

        this.slimeWorld = world;

        SlimePropertyMap propertyMap = world.getPropertyMap();

        this.serverLevelData.setDifficulty(Difficulty.valueOf(propertyMap.getValue(SlimeProperties.DIFFICULTY).toUpperCase()));
        this.serverLevelData.setSpawn(new BlockPos(propertyMap.getValue(SlimeProperties.SPAWN_X), propertyMap.getValue(SlimeProperties.SPAWN_Y), propertyMap.getValue(SlimeProperties.SPAWN_Z)), 0);
        super.setSpawnSettings(propertyMap.getValue(SlimeProperties.ALLOW_MONSTERS), propertyMap.getValue(SlimeProperties.ALLOW_ANIMALS));

        this.pvpMode = propertyMap.getValue(SlimeProperties.PVP);
        {
            String biomeStr = slimeWorld.getPropertyMap().getValue(SlimeProperties.DEFAULT_BIOME);
            ResourceKey<Biome> biomeKey = ResourceKey.create(Registry.BIOME_REGISTRY, new ResourceLocation(biomeStr));
            Holder<Biome> defaultBiome = MinecraftServer.getServer().registryAccess().ownedRegistryOrThrow(Registry.BIOME_REGISTRY).getHolder(biomeKey).orElseThrow();

            this.defaultBiomeSource = new FixedBiomeSource(defaultBiome);
        }

        this.keepSpawnInMemory = false;
    }

    @Override
    public void save(@Nullable ProgressListener progressUpdate, boolean forceSave, boolean savingDisabled) {
        if (!slimeWorld.isReadOnly() && !savingDisabled) {
            Bukkit.getPluginManager().callEvent(new WorldSaveEvent(getWorld()));

            this.getChunkSource().save(forceSave);
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
                byte[] serializedWorld = slimeWorld.serialize().join();
                long saveStart = System.currentTimeMillis();
                slimeWorld.getLoader().saveWorld(slimeWorld.getName(), serializedWorld, false);
                Bukkit.getLogger().log(Level.INFO, "World " + slimeWorld.getName() + " serialized in " + (saveStart - start) + "ms and saved in " + (System.currentTimeMillis() - saveStart) + "ms.");
            } catch (IOException | IllegalStateException ex) {
                ex.printStackTrace();
            }
        }
    }

    ImposterProtoChunk getImposterChunk(int x, int z) {
        SlimeChunk slimeChunk = slimeWorld.getChunk(x, z);
        LevelChunk chunk;

        if (slimeChunk == null) {
            ChunkPos pos = new ChunkPos(x, z);
            LevelChunkTicks<Block> blockLevelChunkTicks = new LevelChunkTicks<>();
            LevelChunkTicks<Fluid> fluidLevelChunkTicks = new LevelChunkTicks<>();

            chunk = new LevelChunk(this, pos, UpgradeData.EMPTY, blockLevelChunkTicks, fluidLevelChunkTicks,
                    0L, null, null, null);

            slimeWorld.updateChunk(new NMSSlimeChunk(null, chunk));
        } else if (slimeChunk instanceof NMSSlimeChunk) {
            chunk = ((NMSSlimeChunk) slimeChunk).getChunk(); // This shouldn't happen anymore, unloading should cleanup the chunk
        } else {
            AtomicReference<NMSSlimeChunk> jank = new AtomicReference<>();
            chunk = convertChunk(slimeChunk, () -> {
                jank.get().dirtySlime();
            });

            NMSSlimeChunk nmsSlimeChunk = new NMSSlimeChunk(slimeChunk, chunk);
            jank.set(nmsSlimeChunk);

            slimeWorld.updateChunk(nmsSlimeChunk);
        }

        return new ImposterProtoChunk(chunk, false);
    }

    private SlimeChunk convertChunk(NMSSlimeChunk chunk) {
        return new CraftSlimeChunk(
                chunk.getWorldName(), chunk.getX(), chunk.getZ(),
                chunk.getSections(), chunk.getHeightMaps(),
                new int[0], chunk.getTileEntities(), new ArrayList<>(),
                chunk.getMinSection(), chunk.getMaxSection());
    }

    private LevelChunk convertChunk(SlimeChunk chunk, Runnable onUnload) {
        int x = chunk.getX();
        int z = chunk.getZ();

        ChunkPos pos = new ChunkPos(x, z);

        // Chunk sections
        LevelChunkSection[] sections = new LevelChunkSection[this.getSectionsCount()];

        Object[] blockNibbles = null;
        Object[] skyNibbles = null;
        if (v1191SlimeNMS.isPaperMC) {
            blockNibbles = ca.spottedleaf.starlight.common.light.StarLightEngine.getFilledEmptyLight(this);
            skyNibbles = ca.spottedleaf.starlight.common.light.StarLightEngine.getFilledEmptyLight(this);
            getServer().scheduleOnMain(() -> {
                getLightEngine().retainData(pos, true);
            });
        }

        Registry<Biome> biomeRegistry = this.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
        // Ignore deprecated method


        Codec<PalettedContainer<Holder<Biome>>> codec = PalettedContainer.codecRW(biomeRegistry.asHolderIdMap(), biomeRegistry.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, biomeRegistry.getHolderOrThrow(Biomes.PLAINS), null);

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            SlimeChunkSection slimeSection = chunk.getSections()[sectionId];

            if (slimeSection != null) {
                BlockState[] presetBlockStates = null;
                if (v1191SlimeNMS.isPaperMC) {
                    NibbleArray blockLight = slimeSection.getBlockLight();
                    if (blockLight != null) {
                        blockNibbles[sectionId] = new SWMRNibbleArray(blockLight.getBacking());
                    }

                    NibbleArray skyLight = slimeSection.getSkyLight();
                    if (skyLight != null) {
                        skyNibbles[sectionId] = new SWMRNibbleArray(skyLight.getBacking());
                    }

                    presetBlockStates = this.chunkPacketBlockController.getPresetBlockStates(this, pos, sectionId << 4); // todo this is for anti xray.. do we need it?
                }

                PalettedContainer<BlockState> blockPalette;
                if (slimeSection.getBlockStatesTag() != null) {
                    Codec<PalettedContainer<BlockState>> blockStateCodec = presetBlockStates == null ? ChunkSerializer.BLOCK_STATE_CODEC : PalettedContainer.codecRW(Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState(), presetBlockStates);
                    DataResult<PalettedContainer<BlockState>> dataresult = blockStateCodec.parse(NbtOps.INSTANCE, Converter.convertTag(slimeSection.getBlockStatesTag())).promotePartial((s) -> {
                        System.out.println("Recoverable error when parsing section " + x + "," + z + ": " + s); // todo proper logging
                    });
                    blockPalette = dataresult.getOrThrow(false, System.err::println); // todo proper logging
                } else {
                    blockPalette = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES, presetBlockStates);
                }

                PalettedContainer<Holder<Biome>> biomePalette;

                if (slimeSection.getBiomeTag() != null) {
                    DataResult<PalettedContainer<Holder<Biome>>> dataresult = codec.parse(NbtOps.INSTANCE, Converter.convertTag(slimeSection.getBiomeTag())).promotePartial((s) -> {
                        System.out.println("Recoverable error when parsing section " + x + "," + z + ": " + s); // todo proper logging
                    });
                    biomePalette = dataresult.getOrThrow(false, System.err::println); // todo proper logging
                } else {
                    biomePalette =new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES);
                }

                LevelChunkSection section = new LevelChunkSection(sectionId << 4, blockPalette, biomePalette);
                sections[sectionId] = section;
            }
        }

        // Keep the chunk loaded at level 33 to avoid light glitches
        // Such a high level will let the server not tick the chunk,
        // but at the same time it won't be completely unloaded from memory
//        getChunkProvider().addTicket(SWM_TICKET, pos, 33, Unit.INSTANCE);


        LevelChunk.PostLoadProcessor loadEntities = (nmsChunk) -> {

            // Load tile entities
//            System.out.println("Loading tile entities for chunk (" + pos.x + ", " + pos.z + ") on world " + slimeWorld.getName());
            List<CompoundTag> tileEntities = chunk.getTileEntities();

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
                        }
                    }
                }
            }
        };

        LevelChunkTicks<Block> blockLevelChunkTicks = new LevelChunkTicks<>();
        LevelChunkTicks<Fluid> fluidLevelChunkTicks = new LevelChunkTicks<>();
        LevelChunk nmsChunk = new LevelChunk(this, pos,
                UpgradeData.EMPTY,
                blockLevelChunkTicks, fluidLevelChunkTicks, 0L, sections, loadEntities, null) {
            @Override
            public void unloadCallback() {
                super.unloadCallback();
                onUnload.run();
            }
        };

        // Height Maps
        EnumSet<Heightmap.Types> heightMapTypes = nmsChunk.getStatus().heightmapsAfter();
        CompoundMap heightMaps = chunk.getHeightMaps().getValue();
        EnumSet<Heightmap.Types> unsetHeightMaps = EnumSet.noneOf(Heightmap.Types.class);

        // Light
       if (v1191SlimeNMS.isPaperMC) {
           nmsChunk.setBlockNibbles((SWMRNibbleArray[]) blockNibbles);
           nmsChunk.setSkyNibbles((SWMRNibbleArray[]) skyNibbles);
       }

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
            slimeWorld.updateChunk(new NMSSlimeChunk(slimeChunk, chunk));
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
        // Spigot Start
        for (BlockEntity tileentity : chunk.getBlockEntities().values()) {
            if (tileentity instanceof Container) {
                // Paper start - this area looks like it can load chunks, change the behavior
                // chests for example can apply physics to the world
                // so instead we just change the active container and call the event
                for (HumanEntity h : Lists.newArrayList(((Container) tileentity).getViewers())) {
                    ((CraftHumanEntity)h).getHandle().closeUnloadedInventory(InventoryCloseEvent.Reason.UNLOADED); // Paper
                }
                // Paper end
            }
        }
        // Spigot End

        chunk.unregisterTickContainerFromLevel(this);
    }
}