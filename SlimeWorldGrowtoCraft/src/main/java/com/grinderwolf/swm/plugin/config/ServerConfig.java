package com.grinderwolf.swm.plugin.config;

import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

@ConfigSerializable
public class ServerConfig {

    @Setting("worldConfig")
    private WorldConfig worldConfig = new WorldConfig();
    @Setting("dataSourcesConfig")
    private DatasourcesConfig datasourcesConfig = new DatasourcesConfig();

    @ConfigSerializable
    public static class WorldConfig {
        @Setting("source")
        private String dataSource = "file";
        @Setting("spawn")
        private String spawn = "0.5, 255, 0.5";
        @Setting("difficulty")
        private String difficulty = "peaceful";
        @Setting("environment")
        private String environment = "NORMAL";
        @Setting("worldType")
        private String worldType = "DEFAULT";
        @Setting("defaultBiome")
        private String defaultBiome = "minecraft:plains";
        @Setting("allowMonsters")
        private boolean allowMonsters = true;
        @Setting("allowAnimals")
        private boolean allowAnimals = true;
        @Setting("dragonBattle")
        private boolean dragonBattle = false;
        @Setting("pvp")
        private boolean pvp = true;
        @Setting("loadOnStartup")
        private boolean loadOnStartup = false;
        @Setting("loadOnWarp")
        private boolean loadOnWarp = false;
        @Setting("readOnly")
        private boolean readOnly = false;

        public boolean isLoadOnWarp() {
            return loadOnWarp;
        }

        public void setLoadOnWarp(boolean loadOnWarp) {
            this.loadOnWarp = loadOnWarp;
        }

        public String getDataSource() {
            return dataSource;
        }

        public void setDataSource(String dataSource) {
            this.dataSource = dataSource;
        }

        public String getSpawn() {
            return spawn;
        }

        public void setSpawn(String spawn) {
            this.spawn = spawn;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(String difficulty) {
            this.difficulty = difficulty;
        }

        public boolean isAllowMonsters() {
            return allowMonsters;
        }

        public void setAllowMonsters(boolean allowMonsters) {
            this.allowMonsters = allowMonsters;
        }

        public boolean isAllowAnimals() {
            return allowAnimals;
        }

        public void setAllowAnimals(boolean allowAnimals) {
            this.allowAnimals = allowAnimals;
        }

        public boolean isDragonBattle() {
            return dragonBattle;
        }

        public void setDragonBattle(boolean dragonBattle) {
            this.dragonBattle = dragonBattle;
        }

        public boolean isPvp() {
            return pvp;
        }

        public void setPvp(boolean pvp) {
            this.pvp = pvp;
        }

        public String getEnvironment() {
            return environment;
        }

        public void setEnvironment(String environment) {
            this.environment = environment;
        }

        public String getWorldType() {
            return worldType;
        }

        public void setWorldType(String worldType) {
            this.worldType = worldType;
        }

        public String getDefaultBiome() {
            return defaultBiome;
        }

        public void setDefaultBiome(String defaultBiome) {
            this.defaultBiome = defaultBiome;
        }

        public boolean isLoadOnStartup() {
            return loadOnStartup;
        }

        public void setLoadOnStartup(boolean loadOnStartup) {
            this.loadOnStartup = loadOnStartup;
        }

        public boolean isReadOnly() {
            return readOnly;
        }

        public void setReadOnly(boolean readOnly) {
            this.readOnly = readOnly;
        }
    }

    @ConfigSerializable
    public static class DatasourcesConfig {

        @Setting("file")
        private FileConfig fileConfig = new FileConfig();
        @Setting("mysql")
        private MysqlConfig mysqlConfig = new MysqlConfig();
        @Setting("mongodb")
        private MongoDBConfig mongoDbConfig = new MongoDBConfig();
        @Setting("redis")
        private RedisConfig redisConfig = new RedisConfig();

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
            private String uri = " redis://{password}@{host}:{port}/0";

            public String getUri() {
                return uri;
            }

            public boolean isEnabled() {
                return enabled;
            }
        }
    }

    public WorldConfig getWorldConfig() {
        return worldConfig;
    }

    public void setWorldConfig(WorldConfig worldConfig) {
        this.worldConfig = worldConfig;
    }

    public DatasourcesConfig getDatasourcesConfig() {
        return datasourcesConfig;
    }

    public void setDatasourcesConfig(DatasourcesConfig datasourcesConfig) {
        this.datasourcesConfig = datasourcesConfig;
    }
}
