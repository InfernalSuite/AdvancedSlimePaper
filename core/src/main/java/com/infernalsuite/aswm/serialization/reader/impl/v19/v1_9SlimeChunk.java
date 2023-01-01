package com.infernalsuite.aswm.serialization.reader.impl.v19;

import com.flowpowered.nbt.CompoundTag;

import java.util.List;

final class v1_9SlimeChunk {
    final String worldName;
    final int x;
    final int z;
    v1_9SlimeChunkSection[] sections;
    final int minY;
    final int maxY;
    final CompoundTag heightMap;
    int[] biomes;
    final List<CompoundTag> tileEntities;
    final List<CompoundTag> entities;

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


