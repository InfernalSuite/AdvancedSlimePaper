package com.infernalsuite.asp.api.world;

import com.infernalsuite.asp.api.utils.NibbleArray;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.Nullable;

/**
 * In-memory representation of a SRF chunk section.
 */
public interface SlimeChunkSection {

    CompoundBinaryTag getBlockStatesTag();

    CompoundBinaryTag getBiomeTag();

    /**
     * Returns the block light data.
     *
     * @return A {@link NibbleArray} with the block light data.
     */
    @Nullable
    NibbleArray getBlockLight();

    /**
     * Returns the sky light data.
     *
     * @return A {@link NibbleArray} containing the sky light data.
     */
    @Nullable
    NibbleArray getSkyLight();
}
