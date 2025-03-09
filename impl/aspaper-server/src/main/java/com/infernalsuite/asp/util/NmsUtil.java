package com.infernalsuite.asp.util;

public class NmsUtil {

    public static long asLong(int chunkX, int chunkZ) {
        return (((long) chunkZ) * Integer.MAX_VALUE + ((long) chunkX));
        //return (long)chunkX & 4294967295L | ((long)chunkZ & 4294967295L) << 32;
    }
}
