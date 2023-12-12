package com.grinderwolf.swm.plugin.loaders;

import com.grinderwolf.swm.plugin.loaders.api.APILoader;
import com.grinderwolf.swm.plugin.loaders.file.FileLoader;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.DatasourcesConfig;
import com.grinderwolf.swm.plugin.loaders.mongo.MongoLoader;
import com.grinderwolf.swm.plugin.loaders.mysql.MysqlLoader;
import com.grinderwolf.swm.plugin.loaders.redis.RedisLoader;
import com.mongodb.MongoException;
import io.lettuce.core.RedisException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class LoaderUtils {

    public static final long MAX_LOCK_TIME = 300000L; // Max time difference between current time millis and world lock
    public static final long LOCK_INTERVAL = 60000L;

    private static final Map<String, SlimeLoader> loaderMap = new HashMap<>();
    private static final Logger LOGGER = LoggerFactory.getLogger(LoaderUtils.class);

    public static void registerLoaders() {
        DatasourcesConfig config = ConfigManager.getDatasourcesConfig();

        // File loader
        DatasourcesConfig.FileConfig fileConfig = config.getFileConfig();
        registerLoader("file", new FileLoader(new File(fileConfig.getPath())));

        // Mysql loader
        DatasourcesConfig.MysqlConfig mysqlConfig = config.getMysqlConfig();
        if (mysqlConfig.isEnabled()) {
            try {
                registerLoader("mysql", new MysqlLoader(mysqlConfig));
            } catch (final SQLException ex) {
                LOGGER.error("Failed to establish connection to the MySQL server:", ex);
            }
        }

        // MongoDB loader
        DatasourcesConfig.MongoDBConfig mongoConfig = config.getMongoDbConfig();

        if (mongoConfig.isEnabled()) {
            try {
                registerLoader("mongodb", new MongoLoader(mongoConfig));
            } catch (final MongoException ex) {
                LOGGER.error("Failed to establish connection to the MongoDB server:", ex);
            }
        }

        DatasourcesConfig.RedisConfig redisConfig = config.getRedisConfig();
        if (redisConfig.isEnabled()){
            try {
                registerLoader("redis", new RedisLoader(redisConfig));
            } catch (final RedisException ex) {
                LOGGER.error("Failed to establish connection to the Redis server:", ex);
            }
        }

        DatasourcesConfig.APIConfig apiConfig = config.getApiConfig();
        if(apiConfig.isEnabled()){
            registerLoader("api", new APILoader(apiConfig));
        }
    }

    public static List<String> getAvailableLoadersNames() {
        return new LinkedList<>(loaderMap.keySet());
    }


    public static SlimeLoader getLoader(String dataSource) {
        return loaderMap.get(dataSource);
    }

    public static void registerLoader(String dataSource, SlimeLoader loader) {
        if (loaderMap.containsKey(dataSource)) {
            throw new IllegalArgumentException("Data source " + dataSource + " already has a declared loader!");
        }

        if (loader instanceof UpdatableLoader) {
            try {
                ((UpdatableLoader) loader).update();
            } catch (final UpdatableLoader.NewerDatabaseException e) {
                LOGGER.error("Data source {} version is {}, while this SWM version only supports up to version {}.",
                        dataSource, e.getDatabaseVersion(), e.getCurrentVersion(), e);
                return;
            } catch (final IOException ex) {
                LOGGER.error("Failed to update data source {}", dataSource, ex);
                return;
            }
        }

        loaderMap.put(dataSource, loader);
    }

}
