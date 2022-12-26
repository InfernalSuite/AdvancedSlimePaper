package com.grinderwolf.swm.plugin.config;

import com.grinderwolf.swm.plugin.log.Logging;
import io.leangen.geantyref.TypeToken;
import lombok.Getter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Getter
@ConfigSerializable
public class WorldsConfig {

    @Setting("worlds")
    private final Map<String, WorldData> worlds = new HashMap<>();

    public void save() {
        try {
            ConfigManager.getWorldConfigLoader().save(ConfigManager.getWorldConfigLoader().createNode().set(TypeToken.get(WorldsConfig.class), this));
        } catch (IOException ex) {
            Logging.error("Failed to save worlds config file:");
            ex.printStackTrace();
        }
    }
}
