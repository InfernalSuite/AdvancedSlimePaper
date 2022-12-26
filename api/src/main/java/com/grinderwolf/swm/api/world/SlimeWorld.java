package com.grinderwolf.swm.api.world;

import com.flowpowered.nbt.CompoundTag;
import com.grinderwolf.swm.api.exceptions.WorldAlreadyExistsException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.experimental.Wither;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * In-memory representation of a SRF world.
 */
public interface SlimeWorld {

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

    /**
     * Returns a {@link Map} with every {@link SlimeChunk} that is
     * currently loaded in this slime world.
     *
     * @return A {@link Map} containing every loaded chunk.
     */
     Map<Long, SlimeChunk> getChunks();

    /**
     * Returns the extra data of the world. Inside this {@link CompoundTag}
     * can be stored any information to then be retrieved later, as it's
     * saved alongside the world data.
     *
     * @return A {@link CompoundTag} containing the extra data of the world.
     */
    CompoundTag getExtraData();

    /**
     * Returns a {@link Collection} with every world map, serialized
     * in a {@link CompoundTag} object.
     *
     * @return A {@link Collection} containing every world map.
     */
    Collection<CompoundTag> getWorldMaps();

    /**
     * Returns the properties of the world. These properties are automatically
     * kept up-to-date when the world is loaded and its properties are updated.
     *
     * @return A {@link SlimeProperties} object with all the current properties of the world.
     * @deprecated see {@link #getPropertyMap()}.
     */
    @Deprecated
    SlimeProperties getProperties();

    /**
     * Returns the property map.
     *
     * @return A {@link SlimePropertyMap} object containing all the properties of the world.
     */
    SlimePropertyMap getPropertyMap();

    /**
     * Returns whether or not read-only is enabled.
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

    /**
     * Returns a clone of the world with the given name. The world will be
     * automatically stored inside the provided data source.
     *
     * @param worldName The name of the cloned world.
     * @param loader The {@link SlimeLoader} used to store the world or <code>null</code> if the world is temporary.
     * @param lock whether or not SWM should lock the world. If false, SWM will not let you load this world for security reasons.
     *
     * @return The clone of the world.
     *
     * @throws IllegalArgumentException if the name of the world is the same as the current one or is <code>null</code>.
     * @throws WorldAlreadyExistsException if there's already a world with the same name inside the provided data source.
     * @throws IOException if the world could not be stored.
     */
    SlimeWorld clone(String worldName, SlimeLoader loader, boolean lock) throws WorldAlreadyExistsException, IOException;

    /**
     * Returns whether or not this world is locked and, therefore, can be loaded on the server by
     * using the {@link com.grinderwolf.swm.api.SlimePlugin#generateWorld(SlimeWorld)} method.
     *
     * @return true if the world is locked, false otherwise
     */
    boolean isLocked();

    /**
     * All the currently-available properties of the world.
     *
     * @deprecated see {@link SlimePropertyMap}
     */
    @Getter
    @Builder(toBuilder = true)
    @Deprecated
    class SlimeProperties {

        private final double spawnX;
        @Builder.Default
        private final double spawnY = 255;
        private final double spawnZ;

        private final int difficulty;

        @Accessors(fluent = true)
        @Builder.Default
        private final boolean allowMonsters = true;
        @Accessors(fluent = true)
        @Builder.Default
        private final boolean allowAnimals = true;

        @Wither
        private final boolean readOnly;

        @Builder.Default
        private final boolean pvp = true;

        @Builder.Default
        private final String environment = "NORMAL";
    }
}
