package com.infernalsuite.aswm.serialization.slime.reader.impl;

import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.serialization.SlimeWorldReader;
import com.infernalsuite.aswm.serialization.slime.reader.VersionedByteSlimeWorldReader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;

public class SimpleWorldFormat<S> implements VersionedByteSlimeWorldReader<SlimeWorld> {

    private final SlimeWorldReader<S> data;
    private final VersionedByteSlimeWorldReader<S> reader;

    public SimpleWorldFormat(SlimeWorldReader<S> data, VersionedByteSlimeWorldReader<S> reader) {
        this.data = data;
        this.reader = reader;
    }

    @Override
    public SlimeWorld deserializeWorld(byte version, @Nullable SlimeLoader loader, String worldName, DataInputStream dataStream, SlimePropertyMap propertyMap, boolean readOnly) throws IOException, CorruptedWorldException, NewerFormatException {
        return this.data.readFromData(this.reader.deserializeWorld(version, loader, worldName, dataStream, propertyMap, readOnly));
    }
}
