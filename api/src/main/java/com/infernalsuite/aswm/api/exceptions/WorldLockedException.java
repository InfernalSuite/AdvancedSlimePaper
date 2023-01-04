package com.infernalsuite.aswm.api.exceptions;

public class WorldLockedException extends SlimeException {
    public WorldLockedException(String worldName) {
        super(worldName);
    }
}
