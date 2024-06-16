package com.infernalsuite.aswm;

public final class Util {

  private Util() {
    throw new AssertionError();
  }

  public static long chunkPosition(final int x, final int z) {
    return ((((long) x) << 32) | (z & 0xFFFFFFFFL));
  }
}
