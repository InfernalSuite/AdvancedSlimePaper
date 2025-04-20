package com.infernalsuite.asp.api.loaders;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@ApiStatus.Experimental
public interface SlimeSerializationAdapter {

    /**
     * Serializes a {@link SlimeWorld} with the current format {@link SlimeSerializationAdapter#getSlimeFormat()}.
     * <p>
     * <strong>If the world is loaded, make sure to use a serializable copy that can be obtained with {@link SlimeWorldInstance#getSerializableCopy()}.</strong>
     *
     * @param slimeWorld World to serialize.
     * @return Serialized world data in bytes.
     * @throws IllegalArgumentException If the world is a {@link SlimeWorldInstance}
     */
    byte[] serializeWorld(@NotNull SlimeWorld slimeWorld);

    /**
     * Deserializes a world from the given byte array and applies data fixers to the world.
     * <p>
     * Unlike {@link AdvancedSlimePaperAPI#readWorld(SlimeLoader, String, boolean, SlimePropertyMap)} this does not
     * save data fixed worlds to the provided {@link SlimeLoader} automatically.
     *
     * @param worldName Name of the world.
     * @param serializedWorld Serialized world data in bytes.
     * @param loader {@link SlimeLoader} used when saving the world.
     * @param propertyMap A {@link SlimePropertyMap} object containing all the properties of the world.
     * @param readOnly Whether read-only mode is enabled.
     * @return A {@link SlimeWorld}, which is the in-memory representation of the world.
     * @throws IOException             if there was a generic problem reading the world.
     * @throws CorruptedWorldException if the world retrieved cannot be properly parsed into a {@link SlimeWorld} object.
     * @throws NewerFormatException    if the world uses a newer version of the SRF.
     */
    @NotNull SlimeWorld deserializeWorld(@NotNull String worldName, byte[] serializedWorld, @Nullable SlimeLoader loader,
                                         @NotNull SlimePropertyMap propertyMap, boolean readOnly)
            throws CorruptedWorldException, NewerFormatException, IOException;

    /**
     * The current slime format version used by AdvancedSlimePaper.
     *
     * @return The slime format version
     */
    int getSlimeFormat();

}
