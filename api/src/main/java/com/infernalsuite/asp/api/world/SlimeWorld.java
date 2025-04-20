package com.infernalsuite.asp.api.world;

import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.persistence.PersistentDataHolder;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory representation of a SRF world.
 */
public interface SlimeWorld extends PersistentDataHolder {

    /**
     * Returns the name of the world.
     *
     * @return The name of the world.
     */
    String getName();

    /**
     * Returns the {@link SlimeLoader} used
     * to load and store the world.
     *
     * @return The {@link SlimeLoader} used to load and store the world.
     */
    SlimeLoader getLoader();

    /**
     * Returns the chunk that belongs to the coordinates specified.
     *
     * @param x X coordinate.
     * @param z Z coordinate.
     *
     * @return The {@link SlimeChunk} that belongs to those coordinates.
     */
     SlimeChunk getChunk(int x, int z);

     Collection<SlimeChunk> getChunkStorage();

    /**
     * Extra data to be stored alongside the world.
     *
     * <p>Any information can be stored inside this map, it will be serialized into a {@link CompoundBinaryTag}
     * and stored alongside the world data so it can then be retrieved later.</p>
     *
     * @apiNote There is a maximum limit of 512 nested tags
     * @implSpec The returned map must be an implementation of {@link ConcurrentMap} to avoid CMEs, etc.
     *
     * @return A Map containing the extra data of the world.
     */
    ConcurrentMap<String, BinaryTag> getExtraData();

    /**
     * Returns a {@link Collection} with every world map, serialized
     * in a {@link CompoundBinaryTag} object.
     *
     * @return A {@link Collection} containing every world map.
     */
    Collection<CompoundBinaryTag> getWorldMaps();

    /**
     * Returns the property map.
     *
     * @return A {@link SlimePropertyMap} object containing all the properties of the world.
     */
    SlimePropertyMap getPropertyMap();

    /**
     * Returns whether read-only is enabled.
     *
     * @return true if read-only is enabled, false otherwise.
     */
    boolean isReadOnly();

    /**
     * Returns a clone of the world with the given name. This world will never be
     * stored, as the <code>readOnly</code> property will be set to true.
     *
     * @param worldName The name of the cloned world.
     *
     * @return The clone of the world.
     *
     * @throws IllegalArgumentException if the name of the world is the same as the current one or is <code>null</code>.
     */
    SlimeWorld clone(String worldName);

    /**
     * Returns a clone of the world with the given name. The world will be
     * automatically stored inside the provided data source.
     *
     * @param worldName The name of the cloned world.
     * @param loader The {@link SlimeLoader} used to store the world or <code>null</code> if the world is temporary.
     *
     * @return The clone of the world.
     *
     * @throws IllegalArgumentException if the name of the world is the same as the current one or is <code>null</code>.
     * @throws WorldAlreadyExistsException if there's already a world with the same name inside the provided data source.
     * @throws IOException if the world could not be stored.
     */
    SlimeWorld clone(String worldName, SlimeLoader loader) throws WorldAlreadyExistsException, IOException;

    int getDataVersion();
}
