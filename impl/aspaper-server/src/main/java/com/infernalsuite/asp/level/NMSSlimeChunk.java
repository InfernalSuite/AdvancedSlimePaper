package com.infernalsuite.asp.level;

import com.google.common.collect.Lists;
import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.PalettedContainerRO;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
    private final CompoundBinaryTag extra;
    private final CompoundBinaryTag upgradeData;

    private final ChunkEntitySlices entitySlices;

    public NMSSlimeChunk(LevelChunk chunk, SlimeChunk reference) {
        this(chunk, reference, null);
    }

    public NMSSlimeChunk(LevelChunk chunk, SlimeChunk reference, ChunkEntitySlices slices) {
        this.chunk = chunk;
        this.extra = reference == null ? CompoundBinaryTag.empty() : reference.getExtraData();
        this.extra.put("ChunkBukkitValues", Converter.convertTag(chunk.persistentDataContainer.toTagCompound()));
        this.upgradeData = reference == null ? null : reference.getUpgradeData();
        this.entitySlices = slices;
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

        Registry<Biome> biomeRegistry = chunk.getLevel().registryAccess().lookupOrThrow(Registries.BIOME);

        // Ignore deprecation, spigot only method
        Codec<PalettedContainerRO<Holder<Biome>>> codec = PalettedContainer.codecRO(biomeRegistry.asHolderIdMap(), biomeRegistry.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, biomeRegistry.get(Biomes.PLAINS).orElseThrow());

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            LevelChunkSection section = chunk.getSections()[sectionId];
            // Sections CANNOT be null in 1.18

            // Block Light Nibble Array
            NibbleArray blockLightArray = Converter.convertArray(lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

            // Sky light Nibble Array
            NibbleArray skyLightArray = Converter.convertArray(lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

            // Tile/Entity Data

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
        List<CompoundTag> tileEntities = new ArrayList<>();

        for (BlockEntity entity : this.chunk.blockEntities.values()) {
            CompoundTag entityNbt = entity.saveWithFullMetadata(net.minecraft.server.MinecraftServer.getServer().registryAccess());
            tileEntities.add(entityNbt);
        }

        return Lists.transform(tileEntities, Converter::convertTag);
    }

    @Override
    public List<CompoundBinaryTag> getEntities() {
        List<CompoundTag> entities = new ArrayList<>();

        ChunkEntitySlices slices = getEntitySlices();
        if (slices == null) return new ArrayList<>();

        // Work by <gunther@gameslabs.net>
        for (Entity entity : slices.entities) {
            CompoundTag entityNbt = new CompoundTag();
            try {
                if (entity.save(entityNbt)) entities.add(entityNbt);
            } catch (final Exception e) {
                LOGGER.error("Could not save the entity = {}, exception = {}", entity, e);
            }
        }

        return Lists.transform(entities, Converter::convertTag);
    }

    private ChunkEntitySlices getEntitySlices() {
        if (this.entitySlices != null) return this.entitySlices;

        if (this.chunk == null || this.chunk.getChunkHolder() == null) {
            return null;
        }

        return this.chunk.getChunkHolder().getEntityChunk();
    }

    @Override
    public CompoundBinaryTag getExtraData() {
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
