package com.infernalsuite.asp.serialization.slime.reader.impl.v1_9;

import com.infernalsuite.asp.api.utils.NibbleArray;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

public class v1_9SlimeChunkSection {

    // Pre 1.13 block data
    public final byte[] blocks;
    public final NibbleArray data;

    // Post 1.13 block data
    public ListBinaryTag palette;
    public final long[] blockStates;

    // Post 1.17 block data
    public CompoundBinaryTag blockStatesTag;
    public CompoundBinaryTag biomeTag;

    public final NibbleArray blockLight;
    public final NibbleArray skyLight;

    public v1_9SlimeChunkSection(byte[] blocks, NibbleArray data, ListBinaryTag palette, long[] blockStates, CompoundBinaryTag blockStatesTag, CompoundBinaryTag biomeTag, NibbleArray blockLight, NibbleArray skyLight) {
        this.blocks = blocks;
        this.data = data;
        this.palette = palette;
        this.blockStates = blockStates;
        this.blockStatesTag = blockStatesTag;
        this.biomeTag = biomeTag;
        this.blockLight = blockLight;
        this.skyLight = skyLight;
    }

}