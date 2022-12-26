package com.grinderwolf.swm.plugin.loaders.slime;

import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.exceptions.NewerFormatException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.utils.SlimeFormat;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.world.SlimeLoadedWorld;
import com.grinderwolf.swm.plugin.loaders.slime.impl.v1_9SlimeWorldFormat;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class SlimeWorldReaderRegistry {

    private static final Map<Byte, SlimeWorldReader> FORMATS = new HashMap<>();

    static {
        register(new v1_9SlimeWorldFormat(), 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }


    private static void register(SlimeWorldReader format, int... bytes) {
        for (int value : bytes) {
            FORMATS.put((byte) value, format);
        }
    }

    public static SlimeLoadedWorld readWorld(SlimeLoader loader, String worldName, byte[] serializedWorld, SlimePropertyMap propertyMap, boolean readOnly) throws IOException, CorruptedWorldException, NewerFormatException {
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

        SlimeWorldReader reader = FORMATS.get(version);
        return reader.deserializeWorld(version, loader, worldName, dataStream, propertyMap, readOnly);
    }

}
