package com.infernalsuite.asp.level;

import ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray;
import ca.spottedleaf.moonrise.patches.starlight.light.StarLightEngine;
import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;

import java.util.EnumSet;
import java.util.List;

public class SlimeChunkConverter {

    static SlimeChunkLevel deserializeSlimeChunk(SlimeLevelInstance instance, SlimeChunk chunk) {
        int x = chunk.getX();
        int z = chunk.getZ();

        ChunkPos pos = new ChunkPos(x, z);

        // Chunk sections
        LevelChunkSection[] sections = new LevelChunkSection[instance.getSectionsCount()];

        SWMRNibbleArray[] blockNibbles = StarLightEngine.getFilledEmptyLight(instance);
        SWMRNibbleArray[] skyNibbles = StarLightEngine.getFilledEmptyLight(instance);
        instance.getServer().scheduleOnMain(() -> {
            instance.getLightEngine().retainData(pos, true);
        });

        Registry<Biome> biomeRegistry = instance.registryAccess().lookupOrThrow(Registries.BIOME);

        Codec<PalettedContainer<Holder<Biome>>> codec = PalettedContainer.codecRW(biomeRegistry.asHolderIdMap(),
                biomeRegistry.holderByNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, biomeRegistry.get(Biomes.PLAINS).orElseThrow(), null);

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            SlimeChunkSection slimeSection = chunk.getSections()[sectionId];

            if (slimeSection != null) {
                NibbleArray blockLight = slimeSection.getBlockLight();
                if (blockLight != null) {
                    blockNibbles[sectionId] = new SWMRNibbleArray(blockLight.getBacking());
                }

                NibbleArray skyLight = slimeSection.getSkyLight();
                if (skyLight != null) {
                    skyNibbles[sectionId] = new SWMRNibbleArray(skyLight.getBacking());
                }

                PalettedContainer<BlockState> blockPalette;
                if (slimeSection.getBlockStatesTag() != null) {
                    DataResult<PalettedContainer<BlockState>> dataresult = SerializableChunkData.BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, Converter.convertTag(slimeSection.getBlockStatesTag())).promotePartial((s) -> {
                        System.out.println("Recoverable error when parsing section " + x + "," + z + ": " + s); // todo proper logging
                    });
                    blockPalette = dataresult.getOrThrow(); // todo proper logging
                } else {
                    blockPalette = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES, null);
                }

                PalettedContainer<Holder<Biome>> biomePalette;

                if (slimeSection.getBiomeTag() != null) {
                    DataResult<PalettedContainer<Holder<Biome>>> dataresult = codec.parse(NbtOps.INSTANCE, Converter.convertTag(slimeSection.getBiomeTag())).promotePartial((s) -> {
                        System.out.println("Recoverable error when parsing section " + x + "," + z + ": " + s); // todo proper logging
                    });
                    biomePalette = dataresult.getOrThrow(); // todo proper logging
                } else {
                    biomePalette = new PalettedContainer<>(biomeRegistry.asHolderIdMap(), biomeRegistry.get(Biomes.PLAINS).orElseThrow(), PalettedContainer.Strategy.SECTION_BIOMES, null);
                }

                if (sectionId < sections.length) {
                    LevelChunkSection section = new LevelChunkSection(blockPalette, biomePalette);
                    sections[sectionId] = section;
                }
            }
        }

        LevelChunkTicks<Block> blockLevelChunkTicks = new LevelChunkTicks<>();
        LevelChunkTicks<Fluid> fluidLevelChunkTicks = new LevelChunkTicks<>();
        UpgradeData upgradeData;
        if (chunk.getUpgradeData() != null) {
            upgradeData = new UpgradeData((net.minecraft.nbt.CompoundTag) Converter.convertTag(chunk.getUpgradeData()), instance);
        } else {
            upgradeData = UpgradeData.EMPTY;
        }

        LevelChunk.PostLoadProcessor processor = SerializableChunkData.postLoadChunk(
                instance,
                chunk.getEntities().stream().map(tag -> (net.minecraft.nbt.CompoundTag) Converter.convertTag(tag)).toList(),
                chunk.getTileEntities().stream().map(tag -> (net.minecraft.nbt.CompoundTag) Converter.convertTag(tag)).toList()
        );

        SlimeChunkLevel nmsChunk = new SlimeChunkLevel(instance, pos, upgradeData, blockLevelChunkTicks,
                fluidLevelChunkTicks, 0L, sections, processor, null);

        // Height Maps
        EnumSet<Heightmap.Types> heightMapTypes = nmsChunk.getPersistedStatus().heightmapsAfter();
        CompoundBinaryTag heightMaps = chunk.getHeightMaps();
        EnumSet<Heightmap.Types> unsetHeightMaps = EnumSet.noneOf(Heightmap.Types.class);

        // Light
        nmsChunk.starlight$setBlockNibbles(blockNibbles);
        nmsChunk.starlight$setSkyNibbles(skyNibbles);

        for (Heightmap.Types type : heightMapTypes) {
            String name = type.getSerializedName();

            long[] heightMap = heightMaps.getLongArray(name);
            if (heightMap.length > 0) {
                nmsChunk.setHeightmap(type, heightMap);
            } else {
                unsetHeightMaps.add(type);
            }
        }

        // Don't try to populate heightmaps if there are none.
        // Does a crazy amount of block lookups
        if (!unsetHeightMaps.isEmpty()) {
            Heightmap.primeHeightmaps(nmsChunk, unsetHeightMaps);
        }

        net.minecraft.nbt.CompoundTag nmsExtraData = (net.minecraft.nbt.CompoundTag) Converter.convertTag(chunk.getExtraData());

        // Attempt to read PDC from the extra tag
        if (nmsExtraData.get("ChunkBukkitValues") != null) {
            nmsChunk.persistentDataContainer.putAll(nmsExtraData.getCompound("ChunkBukkitValues"));
        }

        return nmsChunk;
    }
}
