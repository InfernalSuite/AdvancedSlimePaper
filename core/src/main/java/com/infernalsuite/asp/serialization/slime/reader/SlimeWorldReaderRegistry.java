package com.infernalsuite.asp.serialization.slime.reader;

import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.utils.SlimeFormat;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.serialization.slime.reader.impl.v10.v10WorldFormat;
import com.infernalsuite.asp.serialization.slime.reader.impl.v11.v11WorldFormat;
import com.infernalsuite.asp.serialization.slime.reader.impl.v12.v12WorldFormat;
import com.infernalsuite.asp.serialization.slime.reader.impl.v13.v13WorldFormat;
import com.infernalsuite.asp.serialization.slime.reader.impl.v1_9.v1_9WorldFormat;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SlimeWorldReaderRegistry {

    private static final Map<Byte, VersionedByteSlimeWorldReader<SlimeWorld>> FORMATS = new HashMap<>();

    static {
        register(v1_9WorldFormat.FORMAT, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        register(v10WorldFormat.FORMAT, 10);
        register(v11WorldFormat.FORMAT, 11);
        register(v12WorldFormat.FORMAT, 12);
        register(v13WorldFormat.FORMAT, 13);
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
