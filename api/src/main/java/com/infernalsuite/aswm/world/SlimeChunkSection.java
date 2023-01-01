package com.infernalsuite.aswm.world;

import com.infernalsuite.aswm.utils.NibbleArray;
import com.flowpowered.nbt.CompoundTag;

/**
 * In-memory representation of a SRF chunk section.
 */
public interface SlimeChunkSection {

    CompoundTag getBlockStatesTag();

    CompoundTag getBiomeTag();

    /**
     * Returns the block light data.
     *
     * @return A {@link NibbleArray} with the block light data.
     */
    NibbleArray getBlockLight();

    /**
     * Returns the sky light data.
     *
     * @return A {@link NibbleArray} containing the sky light data.
     */
    NibbleArray getSkyLight();
}
