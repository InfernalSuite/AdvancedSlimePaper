package com.infernalsuite.asp.serialization.slime.reader.impl.v13;

import java.util.EnumSet;

public enum v13AdditionalWorldData {
    POI_CHUNKS,
    BLOCK_TICKS,
    FLUID_TICKS;

    public boolean isSet(byte bitset) {
        return ((bitset >> ordinal()) & 1) == 1;
    }

    public static int countUnsupportedFlags(byte bitset) {
        int supportedFlagsMask = 0;
        for (v13AdditionalWorldData data : v13AdditionalWorldData.values()) {
            supportedFlagsMask |= (1 << data.ordinal());
        }
        int unsupportedFlagsMask = bitset & ~supportedFlagsMask;
        return Integer.bitCount(unsupportedFlagsMask);
    }

    public static byte fromSet(EnumSet<v13AdditionalWorldData> set) {
        byte bitset = 0;
        for (v13AdditionalWorldData data : set) {
            bitset = (byte) (bitset | (1 << data.ordinal()));
        }
        return bitset;
    }


}
