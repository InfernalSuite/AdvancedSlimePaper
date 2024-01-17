package com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9;

import com.flowpowered.nbt.CompoundTag;

import java.util.List;

public final class v1_9SlimeChunk {
    public final String worldName;
    public final int x;
    public final int z;
    public v1_9SlimeChunkSection[] sections;
    public final int minY;
    public final int maxY;
    public final CompoundTag heightMap;
    public int[] biomes;
    public final List<CompoundTag> tileEntities;
    public final List<CompoundTag> entities;
    // Used for 1.13 world upgrading
    public CompoundTag upgradeData;

    v1_9SlimeChunk(String worldName,
                   int x,
                   int z,
                   v1_9SlimeChunkSection[] sections,
                   int minY,
                   int maxY,
                   CompoundTag heightMap,
                   int[] biomes,
                   List<CompoundTag> tileEntities,
                   List<CompoundTag> entities) {
        this.worldName = worldName;
        this.x = x;
        this.z = z;
        this.sections = sections;
        this.minY = minY;
        this.maxY = maxY;
        this.heightMap = heightMap;
        this.biomes = biomes;
        this.tileEntities = tileEntities;
        this.entities = entities;
    }

}


