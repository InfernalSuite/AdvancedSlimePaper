package com.infernalsuite.asp.level;

import com.google.common.collect.Lists;
import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import io.papermc.paper.world.ChunkEntitySlices;
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
import net.minecraft.world.level.chunk.storage.ChunkSerializer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.lighting.LevelLightEngine;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NMSSlimeChunk implements SlimeChunk {
    private static final Logger LOGGER = LogUtils.getClassLogger();

    private static final CompoundBinaryTag EMPTY_BLOCK_STATE_PALETTE;
    private static final CompoundBinaryTag EMPTY_BIOME_PALETTE;

    // Optimized empty section serialization
    static {
        {
            PalettedContainer<BlockState> empty = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES, null);
            Tag tag = ChunkSerializer.BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, empty).getOrThrow(false, (error) -> {
                throw new AssertionError(error);
            });

            EMPTY_BLOCK_STATE_PALETTE = Converter.convertTag(tag);
        }
        {
            Registry<Biome> biomes = net.minecraft.server.MinecraftServer.getServer().registryAccess().registryOrThrow(Registries.BIOME);
            PalettedContainer<Holder<Biome>> empty = new PalettedContainer<>(biomes.asHolderIdMap(), biomes.getHolderOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES, null);
            Tag tag = ChunkSerializer.makeBiomeCodec(biomes).encodeStart(NbtOps.INSTANCE, empty).getOrThrow(false, (error) -> {
                throw new AssertionError(error);
            });

            EMPTY_BIOME_PALETTE = Converter.convertTag(tag);
        }
    }

    private LevelChunk chunk;
    private final CompoundBinaryTag extra;
    private final CompoundBinaryTag upgradeData;

    public NMSSlimeChunk(LevelChunk chunk, SlimeChunk reference) {
        this.chunk = chunk;
        this.extra = reference == null ? CompoundBinaryTag.empty() : reference.getExtraData();
        this.upgradeData = reference == null ? null : reference.getUpgradeData();
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

        Registry<Biome> biomeRegistry = chunk.getLevel().registryAccess().registryOrThrow(Registries.BIOME);

        // Ignore deprecation, spigot only method
        Codec<PalettedContainerRO<Holder<Biome>>> codec = PalettedContainer.codecRO(biomeRegistry.asHolderIdMap(), biomeRegistry.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, biomeRegistry.getHolderOrThrow(Biomes.PLAINS));

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
                Tag data = ChunkSerializer.BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, section.getStates()).getOrThrow(false, System.err::println); // todo error handling
                blockStateTag = Converter.convertTag(data);
            }


            CompoundBinaryTag biomeTag;
            PalettedContainer<Holder<Biome>> biomes = (PalettedContainer<Holder<Biome>>) section.getBiomes();
            if (biomes.data.palette().getSize() == 1 && biomes.data.palette().maybeHas((h) -> h.is(Biomes.PLAINS))) {
                biomeTag = EMPTY_BIOME_PALETTE;
            } else {
                Tag biomeData = codec.encodeStart(NbtOps.INSTANCE, section.getBiomes()).getOrThrow(false, System.err::println); // todo error handling
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
            CompoundTag entityNbt = entity.saveWithFullMetadata();
            tileEntities.add(entityNbt);
        }

        return Lists.transform(tileEntities, Converter::convertTag);
    }

    @Override
    public List<CompoundBinaryTag> getEntities() {
        List<CompoundTag> entities = new ArrayList<>();

        if (this.chunk == null || this.chunk.getChunkHolder() == null) return new ArrayList<>();

        ChunkEntitySlices slices = this.chunk.getChunkHolder().getEntityChunk();
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
