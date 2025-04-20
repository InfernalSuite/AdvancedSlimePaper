package com.infernalsuite.asp.api.exceptions;

import java.io.File;
import java.nio.file.Path;

/**
 * Exception thrown when a folder does
 * not contain a valid Minecraft world.
 */
public class InvalidWorldException extends SlimeException {

    public InvalidWorldException(Path worldDir, String reason) {
        super("Directory " + worldDir.toString() + " does not contain a valid MC world! " + reason);
    }

    public InvalidWorldException(Path worldDir) {
        super("Directory " + worldDir.toString() + " does not contain a valid MC world!");
    }

    public static InvalidWorldException legacy(File worldDir, String reason) {
        return new InvalidWorldException(worldDir.toPath(), reason);
    }

    public static InvalidWorldException legacy(File worldDir) {
        return new InvalidWorldException(worldDir.toPath());
    }

}
