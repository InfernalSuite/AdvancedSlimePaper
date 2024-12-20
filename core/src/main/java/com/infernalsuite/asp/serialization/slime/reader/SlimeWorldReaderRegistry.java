package com.infernalsuite.asp.serialization.slime.reader;

import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.utils.SlimeFormat;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.serialization.slime.reader.impl.v12.v12WorldFormat;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SlimeWorldReaderRegistry {

    private static final Map<Byte, VersionedByteSlimeWorldReader<SlimeWorld>> FORMATS = new HashMap<>();

    static {
        register(v12WorldFormat.FORMAT, 12);
    }

    private static void register(VersionedByteSlimeWorldReader<SlimeWorld> format, int... bytes) {
        for (int value : bytes) {
            FORMATS.put((byte) value, format);
        }
    }

    public static SlimeWorld readWorld(SlimeLoader loader, String worldName, byte[] serializedWorld, SlimePropertyMap propertyMap, boolean readOnly) throws IOException, CorruptedWorldException, NewerFormatException {
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(serializedWorld));
        byte[] fileHeader = new byte[SlimeFormat.SLIME_HEADER.length];
        dataStream.read(fileHeader);

        if (!Arrays.equals(SlimeFormat.SLIME_HEADER, fileHeader)) {
            throw new CorruptedWorldException(worldName);
        }

        // File version
        byte version = dataStream.readByte();

        if (version > SlimeFormat.SLIME_VERSION) {
            throw new NewerFormatException(version);
        }

        VersionedByteSlimeWorldReader<SlimeWorld> reader = FORMATS.get(version);
        return reader.deserializeWorld(version, loader, worldName, dataStream, propertyMap, readOnly);
    }

}
