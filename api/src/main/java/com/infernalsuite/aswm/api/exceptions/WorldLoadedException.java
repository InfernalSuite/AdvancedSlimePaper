package com.infernalsuite.aswm.api.exceptions;

/**
 * Exception thrown when a world is loaded
 * when trying to import it.
 */
public class WorldLoadedException extends SlimeException {

    public WorldLoadedException(String worldName) {
        super("World " + worldName + " is loaded! Unload it before importing.");
    }
}
