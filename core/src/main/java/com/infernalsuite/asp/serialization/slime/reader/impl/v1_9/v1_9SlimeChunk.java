package com.infernalsuite.asp.serialization.slime.reader.impl.v1_9;

import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.List;

public final class v1_9SlimeChunk {
    public final String worldName;
    public final int x;
    public final int z;
    public v1_9SlimeChunkSection[] sections;
    public final int minY;
    public final int maxY;
    public CompoundBinaryTag heightMap;
    public int[] biomes;
    public List<CompoundBinaryTag> tileEntities;
    public List<CompoundBinaryTag> entities;
    // Used for 1.13 world upgrading
    public CompoundBinaryTag upgradeData;

    v1_9SlimeChunk(String worldName,
                   int x,
                   int z,
                   v1_9SlimeChunkSection[] sections,
                   int minY,
                   int maxY,
                   CompoundBinaryTag heightMap,
                   int[] biomes,
                   List<CompoundBinaryTag> tileEntities,
                   List<CompoundBinaryTag> entities) {
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


