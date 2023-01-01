package com.infernalsuite.aswm.serialization.reader.impl;

import com.infernalsuite.aswm.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.exceptions.NewerFormatException;
import com.infernalsuite.aswm.loaders.SlimeLoader;
import com.infernalsuite.aswm.world.SlimeWorld;
import com.infernalsuite.aswm.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.serialization.reader.SlimeWorldReader;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;

public class SimpleWorldFormat<S> implements SlimeWorldReader<SlimeWorld> {

    private final SlimeConverter<S> data;
    private final SlimeWorldReader<S> reader;

    public SimpleWorldFormat(SlimeConverter<S> data, SlimeWorldReader<S> reader) {
        this.data = data;
        this.reader = reader;
    }

    @Override
    public SlimeWorld deserializeWorld(byte version, @Nullable SlimeLoader loader, String worldName, DataInputStream dataStream, SlimePropertyMap propertyMap) throws IOException, CorruptedWorldException, NewerFormatException {
        return this.data.runConversion(this.reader.deserializeWorld(version, loader, worldName, dataStream, propertyMap));
    }
}
