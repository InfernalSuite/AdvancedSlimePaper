package com.infernalsuite.aswm.exceptions;

public class WorldLockedException extends SlimeException {
    public WorldLockedException(String worldName) {
        super(worldName);
    }
}
