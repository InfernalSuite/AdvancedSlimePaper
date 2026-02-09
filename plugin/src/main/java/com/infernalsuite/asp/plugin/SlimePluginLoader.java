package com.infernalsuite.asp.plugin;

import com.infernalsuite.asp.plugin.dependencies.DependenciesMavenResolver;
import com.infernalsuite.asp.plugin.dependencies.DependenciesVersions;
import io.papermc.paper.plugin.bootstrap.PluginProviderContext;
import io.papermc.paper.plugin.loader.PluginClasspathBuilder;
import io.papermc.paper.plugin.loader.PluginLoader;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.Map;

public class SlimePluginLoader implements PluginLoader {

    private static final Path CONFIG_PATH = Path.of("plugins", "SlimeWorldManager", "sources.yml");

    @Override
    public void classloader(PluginClasspathBuilder builder) {
        File configFile = CONFIG_PATH.toFile();

        if (!configFile.exists()) {
            return;
        }

        DependenciesMavenResolver resolver = new DependenciesMavenResolver();
        resolver.addRepositories();

        try (InputStream is = new FileInputStream(configFile)) {
            Map<String, Object> config = new Yaml().load(is);

            loadSqlDependencies(builder.getContext(), config, resolver);
            loadMongoDependencies(config, resolver);
            loadRedisDependencies(config, resolver);

            builder.addLibrary(resolver.getResolver());
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private void loadSqlDependencies(PluginProviderContext context, Map<String, Object> config, DependenciesMavenResolver resolver) {
        if (!isEnabled(config, "sql")) {
            return;
        }

        resolver.addDependency("com.zaxxer", "HikariCP", DependenciesVersions.HIKARI);
        String url = getNestedString(config, "sql", "sqlUrl");
        if (url == null || !url.startsWith("jdbc:")) {
            return;
        }

        String driverType = url.split(":")[1].toLowerCase();

        switch (driverType) {
            case "mysql"      -> resolver.addDependency("com.mysql", "mysql-connector-j", DependenciesVersions.MYSQL);
            case "mariadb"    -> resolver.addDependency("org.mariadb.jdbc", "mariadb-java-client", DependenciesVersions.MARIADB);
            case "postgresql" -> resolver.addDependency("org.postgresql", "postgresql", DependenciesVersions.POSTGRES);
            case "sqlite"     -> resolver.addDependency("org.xerial", "sqlite-jdbc", DependenciesVersions.SQLITE);
            case "h2"         -> resolver.addDependency("com.h2database", "h2", DependenciesVersions.H2);
            default           -> context.getLogger().warn("Unsupported SQL driver type '{}', no SQL dependencies will be loaded.", driverType);
        }
    }

    private void loadMongoDependencies(Map<String, Object> config, DependenciesMavenResolver resolver) {
        if (isEnabled(config, "mongodb")) {
            resolver.addDependency("org.mongodb", "mongodb-driver-sync", DependenciesVersions.MONGODB);
        }
    }

    private void loadRedisDependencies(Map<String, Object> config, DependenciesMavenResolver resolver) {
        if (isEnabled(config, "redis")) {
            resolver.addDependency("io.lettuce", "lettuce-core", DependenciesVersions.LETTUCE);
        }
    }

    private boolean isEnabled(Map<String, Object> config, String key) {
        return config.get(key) instanceof Map<?, ?> section && Boolean.TRUE.equals(section.get("enabled"));
    }

    private String getNestedString(Map<String, Object> config, String sectionKey, String valueKey) {
        return config.get(sectionKey) instanceof Map<?, ?> section ? String.valueOf(section.get(valueKey)) : null;
    }
}