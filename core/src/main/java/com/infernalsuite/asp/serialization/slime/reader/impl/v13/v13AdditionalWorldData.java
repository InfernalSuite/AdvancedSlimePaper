package com.infernalsuite.asp.serialization.slime.reader.impl.v13;

import java.util.EnumSet;

public enum v13AdditionalWorldData {
    BLOCK_TICKS,
    FLUID_TICKS,
    POI_CHUNKS;

    public boolean isSet(byte bitset) {
        return ((bitset >> ordinal()) & 1) == 1;
    }

    public static byte fromSet(EnumSet<v13AdditionalWorldData> set) {
        byte bitset = 0;
        for (v13AdditionalWorldData data : set) {
            bitset = (byte) (bitset | (1 << data.ordinal()));
        }
        return bitset;
    }


}
