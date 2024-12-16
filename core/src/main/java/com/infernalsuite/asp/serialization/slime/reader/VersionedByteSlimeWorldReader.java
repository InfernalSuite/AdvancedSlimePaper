package com.infernalsuite.asp.serialization.slime.reader;

import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;

public interface VersionedByteSlimeWorldReader<T> {

    T deserializeWorld(byte version, @Nullable SlimeLoader loader, String worldName, DataInputStream dataStream, SlimePropertyMap propertyMap, boolean readOnly) throws IOException, CorruptedWorldException, NewerFormatException;
}
