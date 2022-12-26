package com.grinderwolf.swm.plugin.config;

import lombok.Getter;
import lombok.Setter;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@Getter
@ConfigSerializable
public class DatasourcesConfig {

    @Setting("file")
    @Setter
    private FileConfig fileConfig = new FileConfig();
    @Setting("mysql")
    @Setter
    private MysqlConfig mysqlConfig = new MysqlConfig();
    @Setting("mongodb")
    @Setter
    private MongoDBConfig mongoDbConfig = new MongoDBConfig();
    @Setting("redis")
    @Setter
    private RedisConfig redisConfig = new RedisConfig();

    @Getter
    @ConfigSerializable
    public static class MysqlConfig {

        @Setting("enabled")
        @Setter
        private boolean enabled = false;

        @Setting("host")
        @Setter
        private String host = "127.0.0.1";
        @Setting("port")
        @Setter
        private int port = 3306;

        @Setting("username")
        @Setter
        private String username = "slimeworldmanager";
        @Setting("password")
        @Setter
        private String password = "";

        @Setting("database")
        @Setter
        private String database = "slimeworldmanager";

        @Setting("usessl")
        @Setter
        private boolean usessl = false;

        @Setting("sqlUrl")
        @Setter
        private String sqlUrl = "jdbc:mysql://{host}:{port}/{database}?autoReconnect=true&allowMultiQueries=true&useSSL={usessl}";
    }

    @Getter
    @ConfigSerializable
    public static class MongoDBConfig {

        @Setting("enabled")
        @Setter
        private boolean enabled = false;

        @Setting("host")
        @Setter
        private String host = "127.0.0.1";
        @Setting("port")
        @Setter
        private int port = 27017;

        @Setting("auth")
        @Setter
        private String authSource = "admin";
        @Setting("username")
        @Setter
        private String username = "slimeworldmanager";
        @Setting("password")
        @Setter
        private String password = "";

        @Setting("database")
        @Setter
        private String database = "slimeworldmanager";
        @Setting("collection")
        @Setter
        private String collection = "worlds";

        @Setting("uri")
        @Setter
        private String uri = "";
    }

    @Getter
    @ConfigSerializable
    public static class FileConfig {

        @Setting("path")
        @Setter
        private String path = "slime_worlds";

    }

    @Getter
    @ConfigSerializable
    public static class RedisConfig {

        @Setting("enabled")
        @Setter
        private boolean enabled = false;
        @Setting("uri")
        @Setter
        private String uri = "redis://127.0.0.1/";
    }
}
