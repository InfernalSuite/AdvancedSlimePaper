package com.infernalsuite.aswm.world;

import com.flowpowered.nbt.CompoundTag;

import java.util.List;

/**
 * In-memory representation of a SRF chunk.
 */
public interface SlimeChunk {

    /**
     * Returns the X coordinate of the chunk.
     *
     * @return X coordinate of the chunk.
     */
    int getX();

    /**
     * Returns the Z coordinate of the chunk.
     *
     * @return Z coordinate of the chunk.
     */
    int getZ();

    /**
     * Returns all the sections of the chunk.
     *
     * @return A {@link SlimeChunkSection} array.
     */
    SlimeChunkSection[] getSections();

    /**
     * Returns the height maps of the chunk. If it's a pre 1.13 world,
     * a {@link com.flowpowered.nbt.IntArrayTag} containing the height
     * map will be stored inside here by the name of 'heightMap'.
     *
     * @return A {@link CompoundTag} containing all the height maps of the chunk.
     */
    CompoundTag getHeightMaps();

    /**
     * Returns all the tile entities of the chunk.
     *
     * @return A {@link CompoundTag} containing all the tile entities of the chunk.
     */
    List<CompoundTag> getTileEntities();

}
