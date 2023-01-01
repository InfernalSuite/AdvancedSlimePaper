package com.infernalsuite.aswm.exceptions;

/**
 * Exception thrown when a world is locked
 * and is being accessed on write-mode.
 */
public class WorldInUseException extends SlimeException {

    public WorldInUseException(String world) {
        super(world);
    }
}
