package com.infernalsuite.asp.serialization;

import com.infernalsuite.asp.api.SlimeNMSBridge;
import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.loaders.SlimeSerializationAdapter;
import com.infernalsuite.asp.api.utils.SlimeFormat;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.serialization.slime.SlimeSerializer;
import com.infernalsuite.asp.serialization.slime.reader.SlimeWorldReaderRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class SlimeSerializationAdapterImpl implements SlimeSerializationAdapter {

    @Override
    public byte[] serializeWorld(@NotNull SlimeWorld slimeWorld) {
        if(slimeWorld instanceof SlimeWorldInstance) {
            throw new IllegalArgumentException("SlimeWorldInstances cannot be serialized directly. Use SlimeWorldInstance.getSerializableCopy() instead.");
        }
        return SlimeSerializer.serialize(slimeWorld);
    }

    @Override
    public @NotNull SlimeWorld deserializeWorld(@NotNull String worldName, byte[] serializedWorld, @Nullable SlimeLoader loader, @NotNull SlimePropertyMap propertyMap, boolean readOnly) throws CorruptedWorldException, NewerFormatException, IOException {
        SlimeWorld slimeWorld = SlimeWorldReaderRegistry.readWorld(loader, worldName, serializedWorld, propertyMap, loader == null || readOnly);
        return SlimeNMSBridge.instance().getSlimeDataConverter().applyDataFixers(slimeWorld);
    }

    @Override
    public int getSlimeFormat() {
        return SlimeFormat.SLIME_VERSION;
    }

}
