package com.infernalsuite.asp.skeleton;

import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.List;

public record SlimeChunkSkeleton(int x, int z, SlimeChunkSection[] sections,
                                 CompoundBinaryTag heightMap,
                                 List<CompoundBinaryTag> blockEntities,
                                 List<CompoundBinaryTag> entities,
                                 CompoundBinaryTag extra,
                                 CompoundBinaryTag upgradeData) implements SlimeChunk {

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
    public CompoundBinaryTag getExtraData() {
        return this.extra;
    }

    @Override
    public CompoundBinaryTag getUpgradeData() {
        return this.upgradeData;
    }
}
