package com.infernalsuite.asp.api.world;

import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

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
     * Returns the height maps of the chunk.
     *
     * @return A {@link CompoundBinaryTag} containing all the height maps of the chunk.
     */
    @Nullable
    CompoundBinaryTag getHeightMaps();

    /**
     * Returns all the tile entities of the chunk.
     *
     * @return A {@link CompoundBinaryTag} containing all the tile entities of the chunk.
     */
    List<CompoundBinaryTag> getTileEntities();

    /**
     * Returns all the entities of the chunk.
     *
     * @return A {@link CompoundBinaryTag} containing all the entities
     */
    List<CompoundBinaryTag> getEntities();

    /**
     * Returns the extra data of the chunk.
     * Any information can be stored in this {@link Map}
     * and later retrieved, as it's saved alongside the chunk data.
     * <br>
     * <b>Beware, a compound tag under the key "ChunkBukkitValues" will be stored here.
     * It is used for storing chunk-based Bukkit PDC. Do not overwrite it.</b>
     *
     * @return A {@link Map} containing the extra data of the chunk as NBT tags,
     */
    Map<String, BinaryTag> getExtraData();

    /**
     * Upgrade data used to fix the chunks.
     * Not intended to be serialized.
     * @return A {@link CompoundBinaryTag} containing the upgrade data of the chunk,
     */
    @Nullable
    CompoundBinaryTag getUpgradeData();

    /**
     * Returns all block ticks of the chunk. This data is only saved when {@link SlimeProperties#SAVE_BLOCK_TICKS} is true.
     * <p>
     * Even if {@link SlimeProperties#SAVE_BLOCK_TICKS} is false, this might still return
     * a {@link ListBinaryTag} containing the block ticks, if the data was present when the world
     * was read or when the chunk is currently loaded.
     *
     * @return A {@link ListBinaryTag} containing all the block ticks of the chunk, if present.
     * @see SlimeProperties#SAVE_FLUID_TICKS
     */
    @Nullable
    ListBinaryTag getBlockTicks();

    /**
     * Returns all fluid ticks of the chunk. This data is only saved when {@link SlimeProperties#SAVE_FLUID_TICKS} is true.
     * <p>
     * Even if {@link SlimeProperties#SAVE_FLUID_TICKS} is false, this might still return a {@link ListBinaryTag}
     * containing the fluid ticks, if the data was present when the world was read or when the chunk is currently loaded.
     *
     * @return A {@link ListBinaryTag} containing all the fluid ticks of the chunk, if present.
     * @see SlimeProperties#SAVE_FLUID_TICKS
     */
    @Nullable
    ListBinaryTag getFluidTicks();

    /**
     * Returns the poi sections of the chunk. This data is only saved when {@link SlimeProperties#SAVE_POI} is true.
     * <p>
     * Even if {@link SlimeProperties#SAVE_POI} is false, this might still return a {@link CompoundBinaryTag}
     * containing the poi sections, if the data was present when the world was read or when the chunk is currently loaded.
     *
     * @return A {@link CompoundBinaryTag} containing the poi chunks of the chunk, if present.
     * @see SlimeProperties#SAVE_POI
     */
    @Nullable
    CompoundBinaryTag getPoiChunkSections();
}
