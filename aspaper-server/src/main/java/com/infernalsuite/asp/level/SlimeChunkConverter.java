package com.infernalsuite.asp.level;

import ca.spottedleaf.moonrise.patches.chunk_system.level.poi.PoiChunk;
import ca.spottedleaf.moonrise.patches.starlight.light.SWMRNibbleArray;
import ca.spottedleaf.moonrise.patches.starlight.light.StarLightEngine;
import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.storage.SerializableChunkData;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.SavedTick;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class SlimeChunkConverter {

    private static final Codec<List<SavedTick<Block>>> BLOCK_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.BLOCK.byNameCodec()).listOf();
    private static final Codec<List<SavedTick<Fluid>>> FLUID_TICKS_CODEC = SavedTick.codec(BuiltInRegistries.FLUID.byNameCodec()).listOf();

    static SlimeChunkLevel deserializeSlimeChunk(SlimeLevelInstance instance, SlimeChunk chunk) {
        int x = chunk.getX();
        int z = chunk.getZ();

        ChunkPos pos = new ChunkPos(x, z);

        // Chunk sections
        LevelChunkSection[] sections = new LevelChunkSection[instance.getSectionsCount()];

        SWMRNibbleArray[] blockNibbles = StarLightEngine.getFilledEmptyLight(instance);
        SWMRNibbleArray[] skyNibbles = StarLightEngine.getFilledEmptyLight(instance);
        instance.getServer().scheduleOnMain(() -> {
            //TODO: Figure out if this is important. Seems empty in paper
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

        LevelChunkTicks<Block> blockLevelChunkTicks;
        if(chunk.getBlockTicks() != null) {
            ListTag tag = (ListTag) Converter.convertTag(chunk.getBlockTicks());
            List<SavedTick<Block>> blockList = SavedTick.filterTickListForChunk(BLOCK_TICKS_CODEC.parse(NbtOps.INSTANCE, tag).resultOrPartial().orElse(List.of()), pos);

            blockLevelChunkTicks = new LevelChunkTicks<>(blockList);
        } else {
            blockLevelChunkTicks = new LevelChunkTicks<>();
        }

        LevelChunkTicks<Fluid> fluidLevelChunkTicks;
        if(chunk.getFluidTicks() != null) {
            ListTag tag = (ListTag) Converter.convertTag(chunk.getFluidTicks());
            List<SavedTick<Fluid>> fluidList = SavedTick.filterTickListForChunk(FLUID_TICKS_CODEC.parse(NbtOps.INSTANCE, tag).resultOrPartial().orElse(List.of()), pos);

            fluidLevelChunkTicks = new LevelChunkTicks<>(fluidList);
        } else {
            fluidLevelChunkTicks = new LevelChunkTicks<>();
        }


        UpgradeData upgradeData;
        if (chunk.getUpgradeData() != null) {
            upgradeData = new UpgradeData((net.minecraft.nbt.CompoundTag) Converter.convertTag(chunk.getUpgradeData()), instance);
        } else {
            upgradeData = UpgradeData.EMPTY;
        }

        LevelChunk.PostLoadProcessor processor = SerializableChunkData.postLoadChunk(
                instance,
                new ArrayList<>(), //Entities are loaded by moonrise
                chunk.getTileEntities().stream().map(tag -> (net.minecraft.nbt.CompoundTag) Converter.convertTag(tag)).toList()
        );

        SlimeChunkLevel nmsChunk = new SlimeChunkLevel(instance, chunk, pos, upgradeData, blockLevelChunkTicks,
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

        // Attempt to read PDC from the extra tag
        if (chunk.getExtraData().containsKey("ChunkBukkitValues")) {
            nmsChunk.persistentDataContainer.putAll((CompoundTag) Converter.convertTag(chunk.getExtraData().get("ChunkBukkitValues")));
        }

        return nmsChunk;
    }

    public static ListBinaryTag convertSavedFluidTicks(List<SavedTick<Fluid>> ticks) {
        Tag tag = FLUID_TICKS_CODEC.encodeStart(NbtOps.INSTANCE, ticks).getOrThrow();
        return Converter.convertTag(tag);
    }

    public static ListBinaryTag convertSavedBlockTicks(List<SavedTick<Block>> ticks) {
        Tag tag = BLOCK_TICKS_CODEC.encodeStart(NbtOps.INSTANCE, ticks).getOrThrow();
        return Converter.convertTag(tag);
    }

    public static CompoundTag createPoiChunk(SlimeChunk chunk) {
        return createPoiChunkFromSlimeSections(chunk.getPoiChunkSections(),  SharedConstants.getCurrentVersion().dataVersion().version());
    }

    public static CompoundTag createPoiChunkFromSlimeSections(CompoundBinaryTag slimePoiSections, int dataVersion) {
        CompoundTag tag = new CompoundTag();
        tag.put("Sections", Converter.convertTag(slimePoiSections));
        tag.putInt("DataVersion", dataVersion);
        return tag;
    }

    public static CompoundBinaryTag toSlimeSections(PoiChunk poiChunk) {
        CompoundTag save = poiChunk.save();
        return getSlimeSectionsFromPoiCompound(save);
    }

    public static CompoundBinaryTag getSlimeSectionsFromPoiCompound(CompoundTag save) {
        if(save == null) return null;

        CompoundTag sections = save.getCompoundOrEmpty("Sections");
        return Converter.convertTag(sections);
    }
}
