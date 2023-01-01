package com.infernalsuite.aswm.serialization.reader;

import com.infernalsuite.aswm.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.exceptions.NewerFormatException;
import com.infernalsuite.aswm.loaders.SlimeLoader;
import com.infernalsuite.aswm.utils.SlimeFormat;
import com.infernalsuite.aswm.world.SlimeWorld;
import com.infernalsuite.aswm.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.serialization.reader.impl.v19.v1_9WorldFormat;
import com.infernalsuite.aswm.serialization.reader.impl.v10.v10WorldFormat;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SlimeWorldReaderRegistry {

    private static final Map<Byte, SlimeWorldReader<SlimeWorld>> FORMATS = new HashMap<>();

    static {
        register(v1_9WorldFormat.FORMAT, 1, 2, 3, 4, 5, 6, 7, 8, 9);
        register(v10WorldFormat.FORMAT, 10);
    }

    private static void register(SlimeWorldReader<SlimeWorld> format, int... bytes) {
        for (int value : bytes) {
            FORMATS.put((byte) value, format);
        }
    }

    public static SlimeWorld readWorld(SlimeLoader loader, String worldName, byte[] serializedWorld, SlimePropertyMap propertyMap) throws IOException, CorruptedWorldException, NewerFormatException {
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

        SlimeWorldReader<SlimeWorld> reader = FORMATS.get(version);
        return reader.deserializeWorld(version, loader, worldName, dataStream, propertyMap);
    }

}
