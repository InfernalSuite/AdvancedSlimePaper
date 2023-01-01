package com.infernalsuite.aswm.serialization.reader.impl.v19;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.infernalsuite.aswm.utils.NibbleArray;

class v1_9SlimeChunkSection {

    // Post 1.13 block data
    final ListTag<CompoundTag> palette;
    final long[] blockStates;

    // Post 1.17 block data
    CompoundTag blockStatesTag;
    CompoundTag biomeTag;

    final NibbleArray blockLight;
    final NibbleArray skyLight;


    public v1_9SlimeChunkSection(ListTag<CompoundTag> palette, long[] blockStates, CompoundTag blockStatesTag, CompoundTag biomeTag, NibbleArray blockLight, NibbleArray skyLight) {
        this.palette = palette;
        this.blockStates = blockStates;
        this.blockStatesTag = blockStatesTag;
        this.biomeTag = biomeTag;
        this.blockLight = blockLight;
        this.skyLight = skyLight;
    }

}