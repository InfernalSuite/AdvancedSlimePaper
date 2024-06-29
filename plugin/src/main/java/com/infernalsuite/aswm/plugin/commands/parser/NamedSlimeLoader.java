package com.infernalsuite.aswm.plugin.commands.parser;

import com.infernalsuite.aswm.api.loaders.SlimeLoader;

public record NamedSlimeLoader(String name, SlimeLoader slimeLoader) {
    @Override
    public String toString() {
        return name;
    }
}
