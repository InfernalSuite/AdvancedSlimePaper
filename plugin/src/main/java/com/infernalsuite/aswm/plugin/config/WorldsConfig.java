package com.infernalsuite.aswm.plugin.config;

import io.leangen.geantyref.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ConfigSerializable
public class WorldsConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldsConfig.class);

    @Setting("worlds")
    private final Map<String, WorldData> worlds = new HashMap<>();

    public void save() {
        try {
            ConfigManager.getWorldConfigLoader().save(ConfigManager.getWorldConfigLoader().createNode().set(TypeToken.get(WorldsConfig.class), this));
        } catch (IOException ex) {
            LOGGER.error("Failed to save worlds config file", ex);
        }
    }

    public Map<String, WorldData> getWorlds() {
        return worlds;
    }
}
