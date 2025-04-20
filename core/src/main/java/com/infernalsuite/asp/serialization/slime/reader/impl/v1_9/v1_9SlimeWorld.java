package com.infernalsuite.asp.serialization.slime.reader.impl.v1_9;

import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.kyori.adventure.nbt.BinaryTag;

import java.util.concurrent.ConcurrentMap;

public class v1_9SlimeWorld {

    public byte version;

    public final String worldName;
    public final SlimeLoader loader;
    public final Long2ObjectMap<v1_9SlimeChunk> chunks;
    public final ConcurrentMap<String, BinaryTag> extraCompound;
    public final SlimePropertyMap propertyMap;
    public final boolean readOnly;

    public v1_9SlimeWorld(byte version,
                   String worldName,
                   SlimeLoader loader,
                   Long2ObjectMap<v1_9SlimeChunk> chunks,
                   ConcurrentMap<String, BinaryTag> extraCompound,
                   SlimePropertyMap propertyMap,
                   boolean readOnly) {
        this.version = version;
        this.worldName = worldName;
        this.loader = loader;
        this.chunks = chunks;
        this.extraCompound = extraCompound;
        this.propertyMap = propertyMap;
        this.readOnly = readOnly;
    }

    public int getDataVersion() {
        return switch (version) {
            case 0x01 -> 99;//1.8; 99 as 1.8 does not have a dataversion yet and 100 is the first one
            case 0x02 -> 184;//1.9.4
            case 0x03 -> 922;//1.11.2
            case 0x04 -> 1631;//1.13.2
            case 0x05 -> 1976;//1.14.4
            case 0x06 -> 2586;//1.16.5
            case 0x07 -> 2730;//1.17.1
            case 0x08 -> 2975;//1.18
            default -> throw new IllegalStateException("Unexpected value: " + version);
        };
    }

}
