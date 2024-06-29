package com.infernalsuite.aswm.plugin.loader;

import com.infernalsuite.aswm.plugin.config.ConfigManager;
import com.infernalsuite.aswm.plugin.config.DatasourcesConfig;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.loaders.UpdatableLoader;
import com.infernalsuite.aswm.loaders.api.APILoader;
import com.infernalsuite.aswm.loaders.file.FileLoader;
import com.infernalsuite.aswm.loaders.mongo.MongoLoader;
import com.infernalsuite.aswm.loaders.mysql.MysqlLoader;
import com.infernalsuite.aswm.loaders.redis.RedisLoader;
import com.mongodb.MongoException;
import io.lettuce.core.RedisException;
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
        DatasourcesConfig config = ConfigManager.getDatasourcesConfig();

        // File loader
        DatasourcesConfig.FileConfig fileConfig = config.getFileConfig();
        registerLoader("file", new FileLoader(new File(fileConfig.getPath())));

        // Mysql loader
        DatasourcesConfig.MysqlConfig mysqlConfig = config.getMysqlConfig();
        if (mysqlConfig.isEnabled()) {
            try {
                registerLoader("mysql", new MysqlLoader(
                        mysqlConfig.getSqlUrl(),
                        mysqlConfig.getHost(), mysqlConfig.getPort(),
                        mysqlConfig.getDatabase(), mysqlConfig.isUsessl(),
                        mysqlConfig.getUsername(), mysqlConfig.getPassword()
                ));
            } catch (final SQLException ex) {
                LOGGER.error("Failed to establish connection to the MySQL server:", ex);
            }
        }

        // MongoDB loader
        DatasourcesConfig.MongoDBConfig mongoConfig = config.getMongoDbConfig();

        if (mongoConfig.isEnabled()) {
            try {
                registerLoader("mongodb", new MongoLoader(
                        mongoConfig.getDatabase(),
                        mongoConfig.getCollection(),
                        mongoConfig.getUsername(),
                        mongoConfig.getPassword(),
                        mongoConfig.getAuthSource(),
                        mongoConfig.getHost(),
                        mongoConfig.getPort(),
                        mongoConfig.getUri()
                ));
            } catch (final MongoException ex) {
                LOGGER.error("Failed to establish connection to the MongoDB server:", ex);
            }
        }

        DatasourcesConfig.RedisConfig redisConfig = config.getRedisConfig();
        if (redisConfig.isEnabled()){
            try {
                registerLoader("redis", new RedisLoader(redisConfig.getUri()));
            } catch (final RedisException ex) {
                LOGGER.error("Failed to establish connection to the Redis server:", ex);
            }
        }

        DatasourcesConfig.APIConfig apiConfig = config.getApiConfig();
        if(apiConfig.isEnabled()){
            registerLoader("api", new APILoader(
                    apiConfig.getUrl(),
                    apiConfig.getUsername(),
                    apiConfig.getToken(),
                    apiConfig.isIgnoreSslCertificate()
            ));
        }
    }

    public void registerLoader(String dataSource, SlimeLoader loader) {
        if (loaders.containsKey(dataSource)) {
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

        loaders.put(dataSource, loader);
    }

    public SlimeLoader getLoader(String dataSource) {
        return loaders.get(dataSource);
    }

    public Map<String, SlimeLoader> getLoaders() {
        return loaders;
    }
}
