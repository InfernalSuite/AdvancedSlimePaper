package com.infernalsuite.asp.loaders.mysql;

import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.loaders.UpdatableLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class MysqlLoader extends UpdatableLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MysqlLoader.class);

    private static final int CURRENT_DB_VERSION = 1;

    // Database version handling queries
    private static String CREATE_VERSIONING_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS `%config_versioning_table_name%` (`id` INT NOT NULL AUTO_INCREMENT, " + "`version` INT(11), PRIMARY KEY(id));";
    private static String INSERT_VERSION_QUERY = "INSERT INTO `%config_versioning_table_name%` (`id`, `version`) VALUES (1, ?) ON DUPLICATE KEY UPDATE `id` = ?;";
    private static String GET_VERSION_QUERY = "SELECT `version` FROM `%config_versioning_table_name%` WHERE `id` = 1;";

    // v1 update query
    private static String ALTER_LOCKED_COLUMN_QUERY = "ALTER TABLE `%config_worlds_table_name%` CHANGE COLUMN `locked` `locked` BIGINT NOT NULL DEFAULT 0;";

    // World handling queries
    private static String CREATE_WORLDS_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS `%config_worlds_table_name%` (`id` INT NOT NULL AUTO_INCREMENT, " + "`name` VARCHAR(255) UNIQUE, `locked` BIGINT, `world` MEDIUMBLOB, PRIMARY KEY(id));";
    private static String SELECT_WORLD_QUERY = "SELECT `world` FROM `%config_worlds_table_name%` WHERE `name` = ?;";
    private static String UPDATE_WORLD_QUERY = "INSERT INTO `%config_worlds_table_name%` (`name`, `world`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `world` = ?;";
    private static String DELETE_WORLD_QUERY = "DELETE FROM `%config_worlds_table_name%` WHERE `name` = ?;";
    private static String LIST_WORLDS_QUERY = "SELECT `name` FROM `%config_worlds_table_name%`;";

    private final HikariDataSource source;

    public MysqlLoader(String sqlUrl, String host, int port, String database, boolean useSSL, String username, String password) throws SQLException {
        this(sqlUrl, host, port, database, useSSL, username, password, "worlds", "database_version");
    }

    public MysqlLoader(String sqlURL, String host, int port, String database, boolean useSSL, String username, String password, String worldsTable, String versioningTable) throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();

        sqlURL = sqlURL.replace("{host}", host);
        sqlURL = sqlURL.replace("{port}", String.valueOf(port));
        sqlURL = sqlURL.replace("{database}", database);
        sqlURL = sqlURL.replace("{usessl}", String.valueOf(useSSL));

        hikariConfig.setJdbcUrl(sqlURL);
//        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getHost() + ":" + config.getPort() + "/" + config.getDatabase() + "?autoReconnect=true&allowMultiQueries=true&useSSL=" + config.isUsessl());
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

        source = new HikariDataSource(hikariConfig);

        parseTableNames(worldsTable, versioningTable);
        init();
    }

    @ApiStatus.Experimental
    public MysqlLoader(HikariDataSource hikariDataSource) throws SQLException {
        source = hikariDataSource;
        init();
    }

    public void close() {
        if (source != null && !source.isClosed()) {
            source.close();
        }
    }

    @Override
    public void update() throws IOException, NewerStorageException {
        try (Connection con = source.getConnection()) {
            int version;

            try (PreparedStatement statement = con.prepareStatement(GET_VERSION_QUERY);
                 ResultSet set = statement.executeQuery()) {
                version = set.next() ? set.getInt(1) : -1;
            }

            if (version > CURRENT_DB_VERSION) {
                throw new NewerStorageException(CURRENT_DB_VERSION, version);
            }

            if (version < CURRENT_DB_VERSION) {
                LOGGER.warn("Your SWM MySQL database is outdated. The update process will start in 10 seconds.");
                LOGGER.warn("Note that this update might make your database incompatible with older SWM versions.");
                LOGGER.warn("Make sure no other servers with older SWM versions are using this database.");
                LOGGER.warn("Shut down the server to prevent your database from being updated.");

                try {
                    Thread.sleep(10000L);
                } catch (InterruptedException ignored) {
                    LOGGER.info("Update process aborted.");
                    return;
                }

                // Update to v1: alter locked column to store a long
                try (PreparedStatement statement = con.prepareStatement(ALTER_LOCKED_COLUMN_QUERY)) {
                    statement.executeUpdate();
                }

                // Insert/update database version table
                try (PreparedStatement statement = con.prepareStatement(INSERT_VERSION_QUERY)) {
                    statement.setInt(1, CURRENT_DB_VERSION);
                    statement.setInt(2, CURRENT_DB_VERSION);
                    statement.executeUpdate();
                }
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public byte[] readWorld(String worldName) throws UnknownWorldException, IOException {
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(SELECT_WORLD_QUERY)) {
            statement.setString(1, worldName);
            ResultSet set = statement.executeQuery();

            if (!set.next()) {
                throw new UnknownWorldException(worldName);
            }

            return set.getBytes("world");
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public boolean worldExists(String worldName) throws IOException {
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(SELECT_WORLD_QUERY)) {
            statement.setString(1, worldName);
            ResultSet set = statement.executeQuery();

            return set.next();
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public List<String> listWorlds() throws IOException {
        List<String> worldList = new ArrayList<>();

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(LIST_WORLDS_QUERY)) {
            ResultSet set = statement.executeQuery();

            while (set.next()) {
                worldList.add(set.getString("name"));
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }

        return worldList;
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld) throws IOException {
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(UPDATE_WORLD_QUERY)) {
            statement.setString(1, worldName);
            statement.setBytes(2, serializedWorld);
            statement.setBytes(3, serializedWorld);
            statement.executeUpdate();

        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void deleteWorld(String worldName) throws IOException, UnknownWorldException {
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(DELETE_WORLD_QUERY)) {
            statement.setString(1, worldName);

            if (statement.executeUpdate() == 0) {
                throw new UnknownWorldException(worldName);
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    private void init() throws SQLException {
        try (Connection con = source.getConnection()) {
            // Create worlds table
            try (PreparedStatement statement = con.prepareStatement(CREATE_WORLDS_TABLE_QUERY)) {
                statement.execute();
            }

            // Create versioning table
            try (PreparedStatement statement = con.prepareStatement(CREATE_VERSIONING_TABLE_QUERY)) {
                statement.execute();
            }
        }
    }

    private void parseTableNames(String worldsTableName, String versioningTableName) {

        CREATE_VERSIONING_TABLE_QUERY = CREATE_VERSIONING_TABLE_QUERY.replace("%config_versioning_table_name%", versioningTableName);
        INSERT_VERSION_QUERY = INSERT_VERSION_QUERY.replace("%config_versioning_table_name%", versioningTableName);
        GET_VERSION_QUERY = GET_VERSION_QUERY.replace("%config_versioning_table_name%", versioningTableName);

        ALTER_LOCKED_COLUMN_QUERY = ALTER_LOCKED_COLUMN_QUERY.replace("%config_worlds_table_name%", worldsTableName);

        CREATE_WORLDS_TABLE_QUERY = CREATE_WORLDS_TABLE_QUERY.replace("%config_worlds_table_name%", worldsTableName);
        SELECT_WORLD_QUERY = SELECT_WORLD_QUERY.replace("%config_worlds_table_name%", worldsTableName);
        UPDATE_WORLD_QUERY = UPDATE_WORLD_QUERY.replace("%config_worlds_table_name%", worldsTableName);
        DELETE_WORLD_QUERY = DELETE_WORLD_QUERY.replace("%config_worlds_table_name%", worldsTableName);
        LIST_WORLDS_QUERY = LIST_WORLDS_QUERY.replace("%config_worlds_table_name%", worldsTableName);

    }

}
