package com.infernalsuite.asp.level.chunk;

import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.mojang.serialization.Codec;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.*;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.SavedTick;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * Mimics how net.minecraft.world.level.chunk.storage.SerializableChunkData creates a copy of the section data
 * to be serialized async. This has a performance advantage over serializing to nbt and then storing it async later.
 */
public record PartiallySerializedSlimeChunk(
        Registry<Biome> biomeRegistry,

        int x, int z,
        PartiallySerializedSlimeChunkSection[] sections,
        @Nullable CompoundBinaryTag heightMap,

        List<CompoundBinaryTag> blockEntities,
        List<CompoundBinaryTag> entities,
        Map<String, BinaryTag> extra,

        CompoundBinaryTag upgradeData,
        @Nullable CompoundBinaryTag poiChunk,

        @Nullable List<SavedTick<Block>> blockTicks,
        @Nullable List<SavedTick<Fluid>> fluidTicks
) implements SlimeChunk {


    public static PartiallySerializedSlimeChunk of(NMSSlimeChunk slimeChunk, boolean saveBlockTicks, boolean saveFluidTicks, boolean savePoi) {
        LevelChunk chunk = slimeChunk.getChunk();

        Registry<Biome> biomes = chunk.biomeRegistry;
        PartiallySerializedSlimeChunkSection[] sections = new PartiallySerializedSlimeChunkSection[chunk.getSectionsCount()];
        LevelLightEngine lightEngine = chunk.getLevel().getChunkSource().getLightEngine();

        for (int sectionId = 0; sectionId < chunk.getSections().length; sectionId++) {
            LevelChunkSection section = chunk.getSections()[sectionId];
            // Sections CANNOT be null in 1.18

            // Block Light Nibble Array
            NibbleArray blockLightArray = Converter.convertArray(lightEngine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

            // Sky light Nibble Array
            NibbleArray skyLightArray = Converter.convertArray(lightEngine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunk.getPos(), sectionId)));

            sections[sectionId] = new PartiallySerializedSlimeChunkSection(section.copy(), blockLightArray, skyLightArray);
        }
        List<SavedTick<Block>> blockTicks = null;
        List<SavedTick<Fluid>> fluidTicks = null;

        if(saveBlockTicks || saveFluidTicks) {
            ChunkAccess.PackedTicks ticksForSerialization = chunk.getTicksForSerialization(chunk.level.getGameTime());

            if(saveBlockTicks) {
                blockTicks = ticksForSerialization.blocks();
            }
            if(saveFluidTicks) {
                fluidTicks = ticksForSerialization.fluids();
            }
        }
        CompoundBinaryTag serializedPoiChunk = savePoi ? slimeChunk.getPoiChunkSections() : null;
        List<CompoundBinaryTag> entities = slimeChunk.getEntities();

        Map<String, BinaryTag> extra = new HashMap<>(slimeChunk.getExtraData());

        // Serialize Bukkit Values (PDC)
        CompoundBinaryTag adventureTag = Converter.convertTag(chunk.persistentDataContainer.toTagCompound());
        extra.put("ChunkBukkitValues", adventureTag);

        return new PartiallySerializedSlimeChunk(
                biomes,
                chunk.locX,
                chunk.locZ,
                sections,
                slimeChunk.getHeightMaps(),
                slimeChunk.getTileEntities(),
                entities,
                extra,
                slimeChunk.getUpgradeData(),
                serializedPoiChunk,
                blockTicks,
                fluidTicks
        );
    }

    @Override
    public int getX() {
        return this.x;
    }

    @Override
    public int getZ() {
        return this.z;
    }

    @Override
    public SlimeChunkSection[] getSections() {
        Codec<PalettedContainerRO<Holder<Biome>>> codec = PalettedContainer.codecRO(biomeRegistry.asHolderIdMap(), biomeRegistry.holderByNameCodec(),
                PalettedContainer.Strategy.SECTION_BIOMES, biomeRegistry.get(Biomes.PLAINS).orElseThrow());

        SlimeChunkSection[] chunkSections = new SlimeChunkSection[this.sections.length];

        for (int i = 0; i < this.sections.length; i++) {
            PartiallySerializedSlimeChunkSection partial = this.sections[i];

            chunkSections[i] = SlimeChunkConverter.convertChunkSection(codec, partial.section, partial.blockLight, partial.skyLight);
        }

        return chunkSections;
    }

    @Override
    public CompoundBinaryTag getHeightMaps() {
        return this.heightMap;
    }

    @Override
    public List<CompoundBinaryTag> getTileEntities() {
        return this.blockEntities;
    }

    @Override
    public List<CompoundBinaryTag> getEntities() {
        return this.entities;
    }

    @Override
    public Map<String, BinaryTag> getExtraData() {
        return this.extra;
    }

    @Override
    public CompoundBinaryTag getUpgradeData() {
        return this.upgradeData;
    }

    @Override
    public ListBinaryTag getBlockTicks() {
        if(this.blockTicks == null) return null;
        return SlimeChunkConverter.convertSavedBlockTicks(this.blockTicks);
    }

    @Override
    public ListBinaryTag getFluidTicks() {
        if(this.fluidTicks == null) return null;
        return SlimeChunkConverter.convertSavedFluidTicks(this.fluidTicks);
    }

    @Override
    public CompoundBinaryTag getPoiChunkSections() {
        return this.poiChunk;
    }

    record PartiallySerializedSlimeChunkSection(LevelChunkSection section, NibbleArray blockLight, NibbleArray skyLight) {}

}
