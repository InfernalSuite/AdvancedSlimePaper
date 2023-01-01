package com.infernalsuite.aswm.skeleton;

import com.flowpowered.nbt.CompoundTag;
import com.infernalsuite.aswm.world.SlimeChunk;
import com.infernalsuite.aswm.world.SlimeChunkSection;

import java.util.List;

public record SlimeChunkSkeleton(int x, int z, SlimeChunkSection[] sections,
                                 CompoundTag heightMap, List<CompoundTag> blockEntities) implements SlimeChunk {

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
    public CompoundTag getHeightMaps() {
        return this.heightMap;
    }

    @Override
    public List<CompoundTag> getTileEntities() {
        return this.blockEntities;
    }

}
