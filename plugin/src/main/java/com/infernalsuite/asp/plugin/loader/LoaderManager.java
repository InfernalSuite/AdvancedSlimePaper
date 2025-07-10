package com.infernalsuite.asp.plugin.loader;

import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.loaders.UpdatableLoader;
import com.infernalsuite.asp.loaders.api.APILoader;
import com.infernalsuite.asp.loaders.file.FileLoader;
import com.infernalsuite.asp.loaders.mongo.MongoLoader;
import com.infernalsuite.asp.loaders.mysql.MysqlLoader;
import com.infernalsuite.asp.loaders.redis.RedisLoader;
import com.infernalsuite.asp.plugin.SWPlugin;
import com.mongodb.MongoException;
import io.lettuce.core.RedisException;
import org.bukkit.Bukkit;
import org.bukkit.plugin.ServicePriority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
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

        // Mysql loader
        com.infernalsuite.asp.plugin.config.DatasourcesConfig.MysqlConfig mysqlConfig = config.getMysqlConfig();
        if (mysqlConfig.isEnabled()) {
            try {
                registerLoader("mysql", registerLoaderService(MysqlLoader.class, new MysqlLoader(
                        mysqlConfig.getSqlUrl(),
                        mysqlConfig.getHost(), mysqlConfig.getPort(),
                        mysqlConfig.getDatabase(), mysqlConfig.isUsessl(),
                        mysqlConfig.getUsername(), mysqlConfig.getPassword()
                )));
            } catch (final SQLException ex) {
                LOGGER.error("Failed to establish connection to the MySQL server:", ex);
            }
        }

        // MongoDB loader
        com.infernalsuite.asp.plugin.config.DatasourcesConfig.MongoDBConfig mongoConfig = config.getMongoDbConfig();

        if (mongoConfig.isEnabled()) {
            try {
                registerLoader("mongodb", registerLoaderService(MongoLoader.class, new MongoLoader(
                        mongoConfig.getDatabase(),
                        mongoConfig.getCollection(),
                        mongoConfig.getUsername(),
                        mongoConfig.getPassword(),
                        mongoConfig.getAuthSource(),
                        mongoConfig.getHost(),
                        mongoConfig.getPort(),
                        mongoConfig.getUri()
                )));
            } catch (final MongoException ex) {
                LOGGER.error("Failed to establish connection to the MongoDB server:", ex);
            }
        }

        com.infernalsuite.asp.plugin.config.DatasourcesConfig.RedisConfig redisConfig = config.getRedisConfig();
        if (redisConfig.isEnabled()){
            try {
                registerLoader("redis", registerLoaderService(RedisLoader.class, new RedisLoader(redisConfig.getUri())));
            } catch (final RedisException ex) {
                LOGGER.error("Failed to establish connection to the Redis server:", ex);
            }
        }

        com.infernalsuite.asp.plugin.config.DatasourcesConfig.APIConfig apiConfig = config.getApiConfig();
        if(apiConfig.isEnabled()){
            registerLoader("api", registerLoaderService(APILoader.class, new APILoader(
                    apiConfig.getUrl(),
                    apiConfig.getUsername(),
                    apiConfig.getToken(),
                    apiConfig.isIgnoreSslCertificate()
            )));
        }
    }

    private <T> T registerLoaderService(Class<T> clazz, T instance) {
        Bukkit.getServicesManager().register(clazz, instance, SWPlugin.getInstance(), ServicePriority.Normal);
        return instance;
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
