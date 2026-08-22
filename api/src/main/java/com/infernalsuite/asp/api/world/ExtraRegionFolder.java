package com.infernalsuite.asp.api.world;

public record ExtraRegionFolder(String directory, String extraDataKey) {

    public static ExtraRegionFolder parse(String value) {
        int separator = value.indexOf('=');
        if (separator < 0) {
            return new ExtraRegionFolder(value, value);
        }
        return new ExtraRegionFolder(value.substring(0, separator), value.substring(separator + 1));
    }
}
