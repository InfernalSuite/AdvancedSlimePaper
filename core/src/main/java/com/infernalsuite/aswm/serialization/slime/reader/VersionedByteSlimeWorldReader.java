package com.infernalsuite.aswm.serialization.slime.reader;

import com.infernalsuite.aswm.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.exceptions.NewerFormatException;
import com.infernalsuite.aswm.loaders.SlimeLoader;
import com.infernalsuite.aswm.world.properties.SlimePropertyMap;
import org.jetbrains.annotations.Nullable;

import java.io.DataInputStream;
import java.io.IOException;

public interface VersionedByteSlimeWorldReader<T> {

    T deserializeWorld(byte version, @Nullable SlimeLoader loader, String worldName, DataInputStream dataStream, SlimePropertyMap propertyMap) throws IOException, CorruptedWorldException, NewerFormatException;
}
