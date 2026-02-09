package com.infernalsuite.asp.plugin.loader;

import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.loaders.UpdatableLoader;
import com.infernalsuite.asp.loaders.api.APILoader;
import com.infernalsuite.asp.loaders.file.FileLoader;
import com.infernalsuite.asp.loaders.mongo.MongoLoader;
import com.infernalsuite.asp.loaders.sql.SqlLoader;
import com.infernalsuite.asp.loaders.redis.RedisLoader;
import com.infernalsuite.asp.plugin.config.DatasourcesConfig;
import com.infernalsuite.asp.plugin.util.ThrowingSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LoaderManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoaderManager.class);

    private final Map<String, SlimeLoader> loaders = new HashMap<>();

    public LoaderManager() {
        com.infernalsuite.asp.plugin.config.DatasourcesConfig config = com.infernalsuite.asp.plugin.config.ConfigManager.getDatasourcesConfig();

        // File loader
        com.infernalsuite.asp.plugin.config.DatasourcesConfig.FileConfig fileConfig = config.getFileConfig();
        registerLoader("file", new FileLoader(new File(fileConfig.getPath())));

        // Sql loader
        DatasourcesConfig.SqlConfig sqlConfig = config.getSqlConfig();
        if (sqlConfig.isEnabled()) {
            ThrowingSupplier<SqlLoader> sqlSupplier = () -> new SqlLoader(
                    sqlConfig.getSqlUrl(),
                    sqlConfig.getHost(), sqlConfig.getPort(),
                    sqlConfig.getDatabase(), sqlConfig.isUsessl(),
                    sqlConfig.getUsername(), sqlConfig.getPassword()
            );

            try {
                SqlLoader sqlLoader = sqlSupplier.get();
                tryRegisterLoader(sqlLoader.getDatabaseType(), () -> sqlLoader);
            } catch (Exception e) {
                LOGGER.error("Failed to establish connection to the loader:", e);
            }
        }

        // MongoDB loader
        com.infernalsuite.asp.plugin.config.DatasourcesConfig.MongoDBConfig mongoConfig = config.getMongoDbConfig();
        if (mongoConfig.isEnabled()) {
            tryRegisterLoader("mongo", () -> new MongoLoader(
                    mongoConfig.getDatabase(),
                    mongoConfig.getCollection(),
                    mongoConfig.getUsername(),
                    mongoConfig.getPassword(),
                    mongoConfig.getAuthSource(),
                    mongoConfig.getHost(),
                    mongoConfig.getPort(),
                    mongoConfig.getUri()
            ));
        }

        com.infernalsuite.asp.plugin.config.DatasourcesConfig.RedisConfig redisConfig = config.getRedisConfig();
        if (redisConfig.isEnabled()) {
            tryRegisterLoader("redis", () -> new RedisLoader(redisConfig.getUri()));
        }

        com.infernalsuite.asp.plugin.config.DatasourcesConfig.APIConfig apiConfig = config.getApiConfig();
        if(apiConfig.isEnabled()) {
            tryRegisterLoader("api", () -> new APILoader(
                    apiConfig.getUrl(),
                    apiConfig.getUsername(),
                    apiConfig.getToken(),
                    apiConfig.isIgnoreSslCertificate()
            ));
        }

        if (getLoaders().isEmpty()) {
            throw new IllegalStateException("No valid data source configuration found! Please check your config file.");
        }
    }

    private void tryRegisterLoader(String name, ThrowingSupplier<SlimeLoader> supplier) {
        try {
            SlimeLoader loader = supplier.get();
            registerLoader(name, loader);
        } catch (Exception ex) {
            LOGGER.error("Failed to establish connection to the loader:", ex);
        }
    }

    public void registerLoader(String dataSource, SlimeLoader loader) {
        if (loaders.containsKey(dataSource)) {
            throw new IllegalArgumentException("Data source " + dataSource + " already has a declared loader!");
        }

        if (loader instanceof UpdatableLoader) {
            try {
                ((UpdatableLoader) loader).update();
            } catch (final UpdatableLoader.NewerStorageException e) {
                LOGGER.error("Data source {} version is {}, while this loader version only supports up to version {}.",
                        dataSource, e.getStorageVersion(), e.getImplementationVersion(), e);
                return;
            } catch (final IOException ex) {
                LOGGER.error("Failed to update data source {}", dataSource, ex);
                return;
            }
        }

        loaders.put(dataSource, loader);
    }

    public SlimeLoader getLoader(String dataSource) {
        return loaders.get(dataSource);
    }

    public Map<String, SlimeLoader> getLoaders() {
        return loaders;
    }
}
