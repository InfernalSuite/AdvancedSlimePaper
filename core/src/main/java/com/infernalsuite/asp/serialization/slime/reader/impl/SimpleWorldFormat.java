package com.infernalsuite.asp.serialization.slime.reader.impl;

import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.serialization.slime.reader.VersionedByteSlimeWorldReader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;

public class SimpleWorldFormat<S> implements VersionedByteSlimeWorldReader<SlimeWorld> {

    private final com.infernalsuite.asp.serialization.SlimeWorldReader<S> data;
    private final VersionedByteSlimeWorldReader<S> reader;

    public SimpleWorldFormat(com.infernalsuite.asp.serialization.SlimeWorldReader<S> data, VersionedByteSlimeWorldReader<S> reader) {
        this.data = data;
        this.reader = reader;
    }

    @Override
    public SlimeWorld deserializeWorld(byte version, @Nullable SlimeLoader loader, String worldName, DataInputStream dataStream, SlimePropertyMap propertyMap, boolean readOnly) throws IOException, CorruptedWorldException, NewerFormatException {
        return this.data.readFromData(this.reader.deserializeWorld(version, loader, worldName, dataStream, propertyMap, readOnly));
    }
}
