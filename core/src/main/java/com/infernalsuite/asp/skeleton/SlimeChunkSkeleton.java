package com.infernalsuite.asp.skeleton;

import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

import java.util.List;
import java.util.Map;

public record SlimeChunkSkeleton(int x, int z, SlimeChunkSection[] sections,
                                 CompoundBinaryTag heightMap,
                                 List<CompoundBinaryTag> blockEntities,
                                 List<CompoundBinaryTag> entities,
                                 Map<String, BinaryTag> extra,
                                 CompoundBinaryTag upgradeData,
                                 CompoundBinaryTag poiChunk,
                                 ListBinaryTag blockTicks,
                                 ListBinaryTag fluidTicks) implements SlimeChunk {

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
        return this.sections;
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
        return this.blockTicks;
    }

    @Override
    public ListBinaryTag getFluidTicks() {
        return this.fluidTicks;
    }

    @Override
    public CompoundBinaryTag getPoiChunkSections() {
        return this.poiChunk;
    }
}
