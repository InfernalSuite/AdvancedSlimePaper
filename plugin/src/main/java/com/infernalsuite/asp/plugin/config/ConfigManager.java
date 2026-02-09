package com.infernalsuite.asp.plugin.config;

import com.infernalsuite.asp.plugin.SWPlugin;
import io.leangen.geantyref.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.loader.HeaderMode;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ConfigManager {

    private static final File PLUGIN_DIR = new File("plugins", "SlimeWorldManager");
    private static final File WORLDS_FILE = new File(PLUGIN_DIR, "worlds.yml");
    private static final File SOURCES_FILE = new File(PLUGIN_DIR, "sources.yml");

    private static WorldsConfig worldConfig;
    private static YamlConfigurationLoader worldConfigLoader;

    private static DatasourcesConfig datasourcesConfig;

    public static void initialize() throws IOException {
        copyDefaultConfigs();

        worldConfigLoader = YamlConfigurationLoader.builder().file(WORLDS_FILE)
                .nodeStyle(NodeStyle.BLOCK).headerMode(HeaderMode.PRESERVE).build();
        worldConfig = worldConfigLoader.load().get(TypeToken.get(WorldsConfig.class));

        YamlConfigurationLoader datasourcesConfigLoader = YamlConfigurationLoader.builder().file(SOURCES_FILE)
                .nodeStyle(NodeStyle.BLOCK).headerMode(HeaderMode.PRESERVE).build();
        datasourcesConfig = datasourcesConfigLoader.load().get(TypeToken.get(DatasourcesConfig.class));

        migrateSources();

        worldConfig.save();
        datasourcesConfigLoader.save(datasourcesConfigLoader.createNode().set(TypeToken.get(DatasourcesConfig.class), datasourcesConfig));
    }

    private static void copyDefaultConfigs() throws IOException {
        PLUGIN_DIR.mkdirs();

        if (!WORLDS_FILE.exists()) {
            Files.copy(SWPlugin.getInstance().getResource("worlds.yml"), WORLDS_FILE.toPath());
        }

        if (!SOURCES_FILE.exists()) {
            Files.copy(SWPlugin.getInstance().getResource("worlds.yml"), SOURCES_FILE.toPath());
        }
    }

    // Migrate mysql to sql format
    private static void migrateSources() throws IOException {
        if (!SOURCES_FILE.exists()) {
            return;
        }

        YamlConfigurationLoader loader = YamlConfigurationLoader.builder().file(SOURCES_FILE)
                .nodeStyle(NodeStyle.BLOCK).headerMode(HeaderMode.PRESERVE).build();
        var root = loader.load();

        if (!root.node("mysql").virtual()) {
            Logger logger = LoggerFactory.getLogger(ConfigManager.class);
            logger.info("Starting configuration migration for datasources...");

            var mysqlNode = root.node("mysql");
            DatasourcesConfig.SqlConfig sql = new DatasourcesConfig.SqlConfig();
            sql.setHost(mysqlNode.node("host").getString("127.0.0.1"));
            sql.setPort(mysqlNode.node("port").getInt(3306));
            sql.setUsername(mysqlNode.node("username").getString("slimeworldmanager"));
            sql.setPassword(mysqlNode.node("password").getString(""));
            sql.setDatabase(mysqlNode.node("database").getString("slimeworldmanager"));
            sql.setUsessl(mysqlNode.node("usessl").getBoolean(false));

            String defaultUrl = String.format("jdbc:mysql://%s:%d/%s?autoReconnect=true&allowMultiQueries=true&useSSL=%s",
                    sql.getHost(), sql.getPort(), sql.getDatabase(), sql.isUsessl());
            sql.setSqlUrl(mysqlNode.node("sqlUrl").getString(defaultUrl));

            if (datasourcesConfig == null) {
                datasourcesConfig = new DatasourcesConfig();
            }

            datasourcesConfig.setSqlConfig(sql);

            root.node("mysql").raw(null);
            loader.save(root);

            logger.info("Migrated configuration datasources");
        }
    }

    public static DatasourcesConfig getDatasourcesConfig() {
        return datasourcesConfig;
    }

    public static WorldsConfig getWorldConfig() {
        return worldConfig;
    }

    static YamlConfigurationLoader getWorldConfigLoader() {
        return worldConfigLoader;
    }
}
