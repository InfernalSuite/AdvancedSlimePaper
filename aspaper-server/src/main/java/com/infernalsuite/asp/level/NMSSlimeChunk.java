package com.infernalsuite.asp.level;

import ca.spottedleaf.moonrise.patches.chunk_system.level.poi.PoiChunk;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.NewChunkHolder;
import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.util.NmsUtil;
import com.mojang.serialization.Codec;
import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.SavedTick;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.level.storage.TagValueOutput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class NMSSlimeChunk implements SlimeChunk {
    private static final Logger LOGGER = LoggerFactory.getLogger(NMSSlimeChunk.class);

    private static final CompoundBinaryTag EMPTY_BLOCK_STATE_PALETTE;
    private static final CompoundBinaryTag EMPTY_BIOME_PALETTE;

    // Optimized empty section serialization
    static {
        {
            PalettedContainer<BlockState> empty = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES, null);
            Tag tag = SerializableChunkData.BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, empty).getOrThrow();

            EMPTY_BLOCK_STATE_PALETTE = Converter.convertTag(tag);
        }
        {
            Registry<Biome> biomes = net.minecraft.server.MinecraftServer.getServer().registryAccess().lookupOrThrow(Registries.BIOME);
            PalettedContainer<Holder<Biome>> empty = new PalettedContainer<>(biomes.asHolderIdMap(), biomes.get(Biomes.PLAINS).orElseThrow(), PalettedContainer.Strategy.SECTION_BIOMES, null);
            Tag tag = SerializableChunkData.makeBiomeCodec(biomes).encodeStart(NbtOps.INSTANCE, empty).getOrThrow();

            EMPTY_BIOME_PALETTE = Converter.convertTag(tag);
        }
    }

    private LevelChunk chunk;
    private final Map<String, BinaryTag> extra;
    private final CompoundBinaryTag upgradeData;

    public NMSSlimeChunk(LevelChunk chunk, SlimeChunk reference) {
        this.chunk = chunk;
        this.extra = reference == null ? new HashMap<>() : reference.getExtraData();
        this.upgradeData = reference == null ? null : reference.getUpgradeData();
    }

    public void updatePersistentDataContainer() {
        this.extra.put("ChunkBukkitValues", Converter.convertTag(chunk.persistentDataContainer.toTagCompound()));
    }

    @Override
    public int getX() {
        return chunk.getPos().x;
    }

    @Override
    public int getZ() {
        return chunk.getPos().z;
    }

    @Override
    public SlimeChunkSection[] getSections() {
        SlimeChunkSection[] sections = new SlimeChunkSection[this.chunk.getSectionsCount()];
        LevelLightEngine lightEngine = chunk.getLevel().getChunkSource().getLightEngine();

        Registry<Biome> biomeRegistry = chunk.biomeRegistry;

        Codec<PalettedContainerRO<Holder<Biome>>> codec = PalettedContainer.codecRO(biomeRegistry.asHolderIdMap(), biomeRegistry.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, biomeRegistry.get(Biomes.PLAINS).orElseThrow());

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            LevelChunkSection section = chunk.getSections()[sectionId];
            // Sections CANNOT be null in 1.18

            // Block Light Nibble Array
            NibbleArray blockLightArray = Converter.convertArray(lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

            // Sky light Nibble Array
            NibbleArray skyLightArray = Converter.convertArray(lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

            // Block Data
            CompoundBinaryTag blockStateTag;
            if (section.hasOnlyAir()) {
                blockStateTag = EMPTY_BLOCK_STATE_PALETTE;
            } else {
                Tag data = SerializableChunkData.BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, section.getStates()).getOrThrow(); // todo error handling
                blockStateTag = Converter.convertTag(data);
            }


            CompoundBinaryTag biomeTag;
            PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>) section.getBiomes();
            if (biomes.data.palette().getSize() == 1 && biomes.data.palette().maybeHas((h) -> h.is(Biomes.PLAINS))) {
                biomeTag = EMPTY_BIOME_PALETTE;
            } else {
                Tag biomeData = codec.encodeStart(NbtOps.INSTANCE, section.getBiomes()).getOrThrow(); // todo error handling
                biomeTag = Converter.convertTag(biomeData);
            }

            sections[sectionId] = new SlimeChunkSectionSkeleton(blockStateTag, biomeTag, blockLightArray, skyLightArray);
        }

        return sections;
    }



    @Override
    public @Nullable ListBinaryTag getFluidTicks() {
        return SlimeChunkConverter.convertSavedFluidTicks(this.chunk.getTicksForSerialization(chunk.level.getGameTime()).fluids());
    }

    @Override
    public @Nullable CompoundBinaryTag getPoiChunkSections() {
        NewChunkHolder chunkHolder = NmsUtil.getChunkHolder(chunk);
        if(chunkHolder == null) return null;

        PoiChunk slices = chunkHolder.getPoiChunk();
        return getPoiChunkSections(slices);
    }

    public CompoundBinaryTag getPoiChunkSections(PoiChunk poiChunk) {
        return SlimeChunkConverter.toSlimeSections(poiChunk);
    }

    @Override
    public @Nullable ListBinaryTag getBlockTicks() {
        return SlimeChunkConverter.convertSavedBlockTicks(this.chunk.getTicksForSerialization(chunk.level.getGameTime()).blocks());
    }

    @Override
    public CompoundBinaryTag getHeightMaps() {
        CompoundBinaryTag.Builder heightMapsTagBuilder = CompoundBinaryTag.builder();

        this.chunk.heightmaps.forEach((type, map) -> {
            if (type.keepAfterWorldgen()) {
                heightMapsTagBuilder.put(type.name(), LongArrayBinaryTag.longArrayBinaryTag(map.getRawData()));
            }
        });

        return heightMapsTagBuilder.build();
    }

    @Override
    public List<CompoundBinaryTag> getTileEntities() {
        Collection<BlockEntity> blockEntities = this.chunk.blockEntities.values();
        List<CompoundBinaryTag> tileEntities = new ArrayList<>(blockEntities.size());

        for (BlockEntity entity : blockEntities) {
            CompoundTag entityNbt = entity.saveWithFullMetadata(net.minecraft.server.MinecraftServer.getServer().registryAccess());
            tileEntities.add(Converter.convertTag(entityNbt));
        }

        return tileEntities;
    }

    @Override
    public List<CompoundBinaryTag> getEntities() {
        NewChunkHolder chunkHolder = NmsUtil.getChunkHolder(chunk);
        if(chunkHolder == null) return new ArrayList<>();

        ChunkEntitySlices slices = chunkHolder.getEntityChunk();
        return getEntities(slices);
    }

    public List<CompoundBinaryTag> getEntities(ChunkEntitySlices slices) {
        if (slices == null) return new ArrayList<>();
        List<CompoundBinaryTag> entities = new ArrayList<>(slices.entities.size());

        try(final ProblemReporter.ScopedCollector scopedCollector = new ProblemReporter.ScopedCollector(ChunkAccess.problemPath(chunk.getPos()), LOGGER))  {
            // Work by <gunther@gameslabs.net>
            for (Entity entity : slices.entities) {
                try {
                    TagValueOutput tagValueOutput = TagValueOutput.createWithContext(scopedCollector, entity.registryAccess());

                    if (entity.save(tagValueOutput))
                        entities.add(Converter.convertTag(tagValueOutput.buildResult()));
                } catch (final Exception e) {
                    LOGGER.error("Could not save the entity = {}, exception = {}", entity, e);
                }
            }
        }

        return entities;
    }

    @Override
    public Map<String, BinaryTag> getExtraData() {
        return extra;
    }

    @Override
    public CompoundBinaryTag getUpgradeData() {
        return upgradeData;
    }

    public LevelChunk getChunk() {
        return chunk;
    }

    public void setChunk(LevelChunk chunk) {
        this.chunk = chunk;
    }

}
