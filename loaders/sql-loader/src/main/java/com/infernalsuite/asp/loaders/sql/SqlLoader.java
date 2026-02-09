package com.infernalsuite.asp.loaders.sql;

import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.loaders.UpdatableLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SqlLoader extends UpdatableLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlLoader.class);
    private static final Map<String, String> DRIVERS = Map.of(
            "h2", "org.h2.Driver",
            "sqlite", "org.xerial.sqlite.JDBC",
            "mysql", "com.mysql.cj.jdbc.Driver",
            "mariadb", "org.mariadb.jdbc.Driver",
            "postgresql", "org.postgresql.Driver"
    );

    private static final int CURRENT_DB_VERSION = 1;

    // Database version handling queries
    private static final String CREATE_VERSIONING_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS database_version (id INTEGER PRIMARY KEY, version INTEGER NOT NULL)";
    private static final String INSERT_VERSION_QUERY = "INSERT INTO database_version (id, version) VALUES (1, ?)";
    private static final String UPDATE_VERSION_QUERY = "UPDATE database_version SET version = ? WHERE id = 1";
    private static final String GET_VERSION_QUERY = "SELECT version FROM database_version WHERE id = 1";

    // World handling queries
    private static final String CREATE_WORLDS_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS worlds (name VARCHAR(255) PRIMARY KEY, locked BIGINT NOT NULL DEFAULT 0, world BLOB NOT NULL)";
    private static final String SELECT_WORLD_QUERY = "SELECT world FROM worlds WHERE name = ?";
    private static final String INSERT_WORLD_QUERY = "INSERT INTO worlds (name, world) VALUES (?, ?)";
    private static final String UPDATE_WORLD_QUERY = "UPDATE worlds SET world = ? WHERE name = ?";
    private static final String DELETE_WORLD_QUERY = "DELETE FROM worlds WHERE name = ?";
    private static final String LIST_WORLDS_QUERY = "SELECT name FROM worlds";

    private final HikariDataSource source;
    private String databaseType;

    public SqlLoader(String sqlURL, String host, int port, String database, boolean useSSL, String username, String password) throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();

        sqlURL = sqlURL.replace("{host}", host);
        sqlURL = sqlURL.replace("{port}", String.valueOf(port));
        sqlURL = sqlURL.replace("{database}", database);
        sqlURL = sqlURL.replace("{usessl}", String.valueOf(useSSL));

        hikariConfig.setJdbcUrl(sqlURL);
        hikariConfig.setUsername(username);
        hikariConfig.setPassword(password);

        String protocol = sqlURL.split(":")[1].toLowerCase(Locale.ROOT);
        String driverClass = DRIVERS.get(protocol);

        if (driverClass != null) {
            // We have to set a driver for some databases (e.g. h2) because Hikari can't detect it from the URL for some reason
            hikariConfig.setDriverClassName(driverClass);
        }

        if (protocol.equals("mysql") || protocol.equals("mariadb")) {
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
        }

        source = new HikariDataSource(hikariConfig);
        init();
    }

    public SqlLoader(HikariDataSource hikariDataSource) throws SQLException {
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

            if (version < CURRENT_DB_VERSION && version != -1) {
                LOGGER.warn("Your SWM database is outdated. The update process will start in 10 seconds.");
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
                this.updateLockedColumn(con);

                try (PreparedStatement statement = con.prepareStatement(UPDATE_VERSION_QUERY)) {
                    statement.setInt(1, CURRENT_DB_VERSION);
                    statement.executeUpdate();
                }
            } else if (version == -1) {
                // Fresh database, just insert the version
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

            try (ResultSet set = statement.executeQuery()) {
                if (!set.next()) {
                    throw new UnknownWorldException(worldName);
                }

                return set.getBytes("world");
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public boolean worldExists(String worldName) throws IOException {
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(SELECT_WORLD_QUERY)) {
            statement.setString(1, worldName);

            try (ResultSet set = statement.executeQuery()) {
                return set.next();
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public List<String> listWorlds() throws IOException {
        List<String> worldList = new ArrayList<>();

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(LIST_WORLDS_QUERY)) {
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    worldList.add(set.getString("name"));
                }
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }

        return worldList;
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld) throws IOException {
        try (Connection con = source.getConnection()) {
            con.setAutoCommit(false);
            try {
                int updated;
                try (PreparedStatement statement = con.prepareStatement(UPDATE_WORLD_QUERY)) {
                    statement.setBytes(1, serializedWorld);
                    statement.setString(2, worldName);
                    updated = statement.executeUpdate();
                }

                // Workaround upsert because there is no standard SQL syntax for it
                if (updated == 0) {
                    try (PreparedStatement statement = con.prepareStatement(INSERT_WORLD_QUERY)) {
                        statement.setString(1, worldName);
                        statement.setBytes(2, serializedWorld);
                        statement.executeUpdate();
                    }
                }

                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw new IOException(e);
            } finally {
                con.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void saveWorlds(Map<String, byte[]> worlds) throws IOException {
        if (worlds.isEmpty()) {
            return;
        }

        try (Connection con = source.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement update = con.prepareStatement(UPDATE_WORLD_QUERY)) {
                    for (Map.Entry<String, byte[]> entry : worlds.entrySet()) {
                        update.setBytes(1, entry.getValue());
                        update.setString(2, entry.getKey());
                        update.addBatch();
                    }
                    int[] results = update.executeBatch();

                    try (PreparedStatement insert = con.prepareStatement(INSERT_WORLD_QUERY)) {
                        int i = 0;
                        for (Map.Entry<String, byte[]> entry : worlds.entrySet()) {
                            if (results[i++] == 0) {
                                insert.setString(1, entry.getKey());
                                insert.setBytes(2, entry.getValue());
                                insert.addBatch();
                            }
                        }
                        insert.executeBatch();
                    }
                }
                con.commit();
            } catch (SQLException e) {
                con.rollback();
                throw new IOException(e);
            } finally {
                con.setAutoCommit(true);
            }
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

    public String getDatabaseType() {
        return databaseType;
    }

    private void init() throws SQLException {
        try (Connection con = source.getConnection();
             Statement statement = con.createStatement()) {
            this.databaseType = con.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);

            // Create worlds table
            statement.execute(CREATE_WORLDS_TABLE_QUERY);

            // Create versioning table
            statement.execute(CREATE_VERSIONING_TABLE_QUERY);
        }
    }

    private void updateLockedColumn(Connection con) throws SQLException {
        String db = databaseType.toLowerCase(Locale.ROOT);
        // Migration is needed only from these databases
        if (!db.contains("mysql") && !db.contains("mariadb")) {
           return;
        }

        try (Statement statement = con.createStatement()) {
            statement.executeUpdate("ALTER TABLE worlds CHANGE COLUMN locked locked BIGINT NOT NULL DEFAULT 0");
        }
    }
}
