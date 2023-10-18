package com.infernalsuite.aswm.api.loaders;

import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.world.ActiveSlimeWorld;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public interface SlimeFormatAdapter {
    /**
     * Serializes a {@link SlimeWorld}.
     * <p>
     * <strong>Important: If the world is active, make sure to use the result of {@link ActiveSlimeWorld#getSnapshot()} as input.</strong>
     *
     * @param world World to serialize.
     * @return Byte array.
     */
    byte[] serialize(@NotNull SlimeWorld world);

    /**
     * Deserializes a {@link SlimeWorld} from a byte array buffer.
     * This will prepare the world so that it's ready to be registered (i.e. apply data fixers).
     *
     * @param worldName Name the loaded world will assume.
     * @param serializedData Serialized data.
     * @param loader The {@link SlimeLoader} that should be used to save the world once loaded. Can be null.
     * @param propertyMap A {@link SlimePropertyMap} with the world properties.
     * @param readOnly Whether the world should be marked as read-only (won't save automatically).
     * @return In-memory slime world.
     * @throws IOException             if something wrong happened while reading the serialized data.
     * @throws CorruptedWorldException if the world retrieved cannot be parsed into a {@link SlimeWorld} object.
     * @throws NewerFormatException    if the world uses a newer version of the SRF.
     */
    @NotNull SlimeWorld deserialize(@NotNull String worldName, byte[] serializedData, @Nullable SlimeLoader loader,
                                    @NotNull SlimePropertyMap propertyMap, boolean readOnly)
            throws IOException, CorruptedWorldException, NewerFormatException;
}
