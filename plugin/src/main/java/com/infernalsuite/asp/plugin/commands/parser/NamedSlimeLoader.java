package com.infernalsuite.asp.plugin.commands.parser;

import com.infernalsuite.asp.api.loaders.SlimeLoader;

public record NamedSlimeLoader(String name, SlimeLoader slimeLoader) {
    @Override
    public String toString() {
        return name;
    }
}
