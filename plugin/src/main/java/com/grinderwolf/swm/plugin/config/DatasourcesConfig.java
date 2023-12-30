package com.grinderwolf.swm.plugin.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class DatasourcesConfig {

    @Setting("file")
    private FileConfig fileConfig = new FileConfig();
    @Setting("mysql")
    private MysqlConfig mysqlConfig = new MysqlConfig();
    @Setting("mongodb")
    private MongoDBConfig mongoDbConfig = new MongoDBConfig();
    @Setting("redis")
    private RedisConfig redisConfig = new RedisConfig();

    @Setting("api")
    private APIConfig apiConfig = new APIConfig();

    @ConfigSerializable
    public static class MysqlConfig {

        @Setting("enabled")
        private boolean enabled = false;

        @Setting("host")
        private String host = "127.0.0.1";
        @Setting("port")
        private int port = 3306;

        @Setting("username")
        private String username = "slimeworldmanager";
        @Setting("password")
        private String password = "";

        @Setting("database")
        private String database = "slimeworldmanager";

        @Setting("usessl")
        private boolean usessl = false;

        @Setting("sqlUrl")
        private String sqlUrl = "jdbc:mysql://{host}:{port}/{database}?autoReconnect=true&allowMultiQueries=true&useSSL={usessl}";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public boolean isUsessl() {
            return usessl;
        }

        public void setUsessl(boolean usessl) {
            this.usessl = usessl;
        }

        public String getSqlUrl() {
            return sqlUrl;
        }

        public void setSqlUrl(String sqlUrl) {
            this.sqlUrl = sqlUrl;
        }
    }

    @ConfigSerializable
    public static class MongoDBConfig {

        @Setting("enabled")
        private boolean enabled = false;

        @Setting("host")
        private String host = "127.0.0.1";
        @Setting("port")
        private int port = 27017;

        @Setting("auth")
        private String authSource = "admin";
        @Setting("username")
        private String username = "slimeworldmanager";
        @Setting("password")
        private String password = "";

        @Setting("database")
        private String database = "slimeworldmanager";
        @Setting("collection")
        private String collection = "worlds";

        @Setting("uri")
        private String uri = "";


        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getAuthSource() {
            return authSource;
        }

        public void setAuthSource(String authSource) {
            this.authSource = authSource;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }

    @ConfigSerializable
    public static class FileConfig {

        @Setting("path")
        private String path = "slime_worlds";

        public String getPath() {
            return path;
        }
    }

    @ConfigSerializable
    public static class RedisConfig {

        @Setting("enabled")
        private boolean enabled = false;
        @Setting("uri")
        private String uri = "redis://127.0.0.1/";

        public String getUri() {
            return uri;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    @ConfigSerializable
    public static class APIConfig {
        @Setting("enabled")
        private boolean enabled = false;

        @Setting("ignoreSslCertificate")
        private boolean ignoreSslCertificate = false;

        @Setting("username")
        private String username = "";
        @Setting("token")
        private String token = "";
        @Setting("url")
        private String url = "";

        public boolean isEnabled() {
            return enabled;
        }

        public boolean isIgnoreSslCertificate() {
            return ignoreSslCertificate;
        }

        public String getUsername() {
            return username;
        }

        public String getToken() {
            return token;
        }

        public String getUrl() {
            return url;
        }
    }

    public FileConfig getFileConfig() {
        return fileConfig;
    }

    public void setFileConfig(FileConfig fileConfig) {
        this.fileConfig = fileConfig;
    }

    public MysqlConfig getMysqlConfig() {
        return mysqlConfig;
    }

    public void setMysqlConfig(MysqlConfig mysqlConfig) {
        this.mysqlConfig = mysqlConfig;
    }

    public MongoDBConfig getMongoDbConfig() {
        return mongoDbConfig;
    }

    public void setMongoDbConfig(MongoDBConfig mongoDbConfig) {
        this.mongoDbConfig = mongoDbConfig;
    }

    public RedisConfig getRedisConfig() {
        return redisConfig;
    }

    public void setRedisConfig(RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

    public APIConfig getApiConfig() {
        return apiConfig;
    }

    public void setApiConfig(APIConfig apiConfig) {
        this.apiConfig = apiConfig;
    }
}
