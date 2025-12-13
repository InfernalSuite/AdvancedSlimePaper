package com.infernalsuite.asp.loaders.postgresql;


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

public class PostgresqlLoader extends UpdatableLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresqlLoader.class);
    private static final int CURRENT_DB_VERSION = 1;

    // Database version handling queries
    private static final String CREATE_VERSIONING_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS database_version (" +
                    "id SERIAL PRIMARY KEY, " +
                    "version INT" +
                    ");";

    private static final String INSERT_VERSION_QUERY =
            "INSERT INTO database_version (id, version) " +
                    "VALUES (1, ?) " +
                    "ON CONFLICT (id) DO UPDATE SET version = EXCLUDED.version;";

    private static final String GET_VERSION_QUERY =
            "SELECT version FROM database_version WHERE id = 1;";

    // World handling queries
    private static final String CREATE_WORLDS_TABLE_QUERY =
            "CREATE TABLE IF NOT EXISTS worlds (" +
                    "id SERIAL PRIMARY KEY, " +
                    "name VARCHAR(255) UNIQUE, " +
                    "locked BIGINT DEFAULT 0 NOT NULL, " +
                    "world BYTEA" +
                    ");";

    private static final String SELECT_WORLD_QUERY =
            "SELECT world FROM worlds WHERE name = ?;";

    private static final String UPDATE_WORLD_QUERY =
            "INSERT INTO worlds (name, world) VALUES (?, ?) " +
                    "ON CONFLICT (name) DO UPDATE SET world = EXCLUDED.world;";

    private static final String DELETE_WORLD_QUERY =
            "DELETE FROM worlds WHERE name = ?;";

    private static final String LIST_WORLDS_QUERY =
            "SELECT name FROM worlds;";

    private final HikariDataSource source;

    public PostgresqlLoader(String sqlURL, String host, int port, String database, boolean useSSL, String username, String password) throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();

        sqlURL = sqlURL.replace("{host}", host)
                .replace("{port}", String.valueOf(port))
                .replace("{database}", database)
                .replace("{usessl}", String.valueOf(useSSL));

        hikariConfig.setJdbcUrl(sqlURL);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);
        hikariConfig.setDriverClassName("org.postgresql.Driver");

        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        source = new HikariDataSource(hikariConfig);
        init();
    }

    @ApiStatus.Experimental
    public PostgresqlLoader(HikariDataSource hikariDataSource) throws SQLException {
        source = hikariDataSource;
        init();
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
                LOGGER.warn("Your PostgreSQL database is outdated. Updating now...");
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException ignored) {
                }

                // Insert/update database version
                try (PreparedStatement statement = con.prepareStatement(INSERT_VERSION_QUERY)) {
                    statement.setInt(1, CURRENT_DB_VERSION);
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
            try (PreparedStatement statement = con.prepareStatement(CREATE_WORLDS_TABLE_QUERY)) {
                statement.execute();
            }
            try (PreparedStatement statement = con.prepareStatement(CREATE_VERSIONING_TABLE_QUERY)) {
                statement.execute();
            }
        }
    }
}
