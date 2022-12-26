package com.grinderwolf.swm.api.exceptions;

/**
 * Generic SWM exception.
 */
public class SlimeException extends Exception {

    public SlimeException(String message) {
        super(message);
    }

    public SlimeException(String message, Exception ex) {
        super(message, ex);
    }
}
