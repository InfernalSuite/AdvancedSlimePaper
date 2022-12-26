package com.grinderwolf.swm.api.exceptions;

/**
 * Exception thrown when a MC world is
 * too big to be converted into the SRF.
 */
public class WorldTooBigException extends SlimeException {

    public WorldTooBigException(String worldName) {
        super("World " + worldName + " is too big to be converted into the SRF!");
    }
}
