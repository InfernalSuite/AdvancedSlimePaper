package com.infernalsuite.aswm.serialization.slime;

import com.infernalsuite.aswm.api.SlimeNMSBridge;
import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.loaders.SlimeFormatAdapter;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.serialization.slime.reader.SlimeWorldReaderRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.function.Consumer;

public class DefaultSlimeFormatAdapter implements SlimeFormatAdapter {
    private final @Nullable Consumer<String> infoLog;

    public DefaultSlimeFormatAdapter(@Nullable Consumer<String> infoLog) {
        this.infoLog = infoLog;
    }

    @Override
    public byte[] serialize(@NotNull SlimeWorld world) {
        return SlimeSerializer.serialize(world);
    }

    @Override
    public @NotNull SlimeWorld deserialize(@NotNull String worldName, byte[] serializedData, @Nullable SlimeLoader loader,
                                           @NotNull SlimePropertyMap propertyMap, boolean readOnly)
            throws CorruptedWorldException, NewerFormatException, IOException {
        SlimeWorld slimeWorld = SlimeWorldReaderRegistry.readWorld(loader, worldName, serializedData, propertyMap, readOnly);
        if (infoLog != null) {
            infoLog.accept("Applying data fixers for " + worldName + ".");
        }
        return SlimeNMSBridge.instance().applyDataFixers(slimeWorld);
    }
}
