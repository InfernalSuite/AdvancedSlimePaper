package com.grinderwolf.swm.plugin.config;

import com.grinderwolf.swm.plugin.SWMPlugin;
import io.leangen.geantyref.TypeToken;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ConfigManager {

    private static final File PLUGIN_DIR = new File("plugins", "SlimeWorldManager");
    private static final File WORLDS_FILE = new File(PLUGIN_DIR, "worlds.yml");
    private static final File SERVERCONF_FILE = new File(PLUGIN_DIR, "config.yml");

    private static WorldsConfig worldConfig;
    private static YamlConfigurationLoader worldConfigLoader;

    private static ServerConfig serverConfig;

    public static void initialize() throws IOException {
        copyDefaultConfigs();

        YamlConfigurationLoader datasourcesConfigLoader = YamlConfigurationLoader.builder().file(SERVERCONF_FILE)
                .nodeStyle(NodeStyle.BLOCK).headerMode(HeaderMode.PRESERVE).build();
        serverConfig = datasourcesConfigLoader.load().get(TypeToken.get(ServerConfig.class));

        worldConfigLoader = YamlConfigurationLoader.builder().file(WORLDS_FILE)
                .nodeStyle(NodeStyle.BLOCK).headerMode(HeaderMode.PRESERVE).build();
        worldConfig = worldConfigLoader.load().get(TypeToken.get(WorldsConfig.class));

        datasourcesConfigLoader.save(datasourcesConfigLoader.createNode().set(TypeToken.get(ServerConfig.class), serverConfig));
        worldConfig.save();
    }

    private static void copyDefaultConfigs() throws IOException {
        PLUGIN_DIR.mkdirs();

        if (!WORLDS_FILE.exists()) {
            Files.copy(SWMPlugin.getInstance().getResource("worlds.yml"), WORLDS_FILE.toPath());
        }

        if (!SERVERCONF_FILE.exists()) {
            Files.copy(SWMPlugin.getInstance().getResource("config.yml"), SERVERCONF_FILE.toPath());
        }
    }

    public static ServerConfig getDatasourcesConfig() {
        return serverConfig;
    }

    public static WorldsConfig getWorldConfig() {
        return worldConfig;
    }

    static YamlConfigurationLoader getWorldConfigLoader() {
        return worldConfigLoader;
    }
}
