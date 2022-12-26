package com.grinderwolf.swm.api.exceptions;

/**
 * Exception thrown when a world is encoded
 * using a newer SRF format than the one that
 * SWM supports.
 */
public class NewerFormatException extends SlimeException {

    public NewerFormatException(byte version) {
        super("v" + version);
    }
}
