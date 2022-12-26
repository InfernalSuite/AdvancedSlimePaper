package com.grinderwolf.swm.plugin.loaders;

import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.DatasourcesConfig;
import com.grinderwolf.swm.plugin.loaders.file.FileLoader;
import com.grinderwolf.swm.plugin.loaders.mongo.MongoLoader;
import com.grinderwolf.swm.plugin.loaders.mysql.MysqlLoader;
import com.grinderwolf.swm.plugin.loaders.redis.RedisLoader;
import com.grinderwolf.swm.plugin.log.Logging;
import com.mongodb.MongoException;
import io.lettuce.core.RedisException;

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
            } catch (SQLException ex) {
                Logging.error("Failed to establish connection to the MySQL server:");
                ex.printStackTrace();
            }
        }

        // MongoDB loader
        DatasourcesConfig.MongoDBConfig mongoConfig = config.getMongoDbConfig();

        if (mongoConfig.isEnabled()) {
            try {
                registerLoader("mongodb", new MongoLoader(mongoConfig));
            } catch (MongoException ex) {
                Logging.error("Failed to establish connection to the MongoDB server:");
                ex.printStackTrace();
            }
        }

        DatasourcesConfig.RedisConfig redisConfig = config.getRedisConfig();
        if(redisConfig.isEnabled()){
          try{
            registerLoader("redis", new RedisLoader(redisConfig));
          }catch (RedisException ex){
            Logging.error("Failed to establish connection to the Redis server:");
            ex.printStackTrace();
          }
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
            } catch (UpdatableLoader.NewerDatabaseException e) {
                Logging.error("Data source " + dataSource + " version is " + e.getDatabaseVersion() + ", while" +
                        " this SWM version only supports up to version " + e.getCurrentVersion() + ".");
                return;
            } catch (IOException ex) {
                Logging.error("Failed to check if data source " + dataSource + " is updated:");
                ex.printStackTrace();
                return;
            }
        }

        loaderMap.put(dataSource, loader);
    }

}
