package com.grinderwolf.swm.plugin.loaders.mysql;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.plugin.config.DatasourcesConfig;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.loaders.UpdatableLoader;
import com.grinderwolf.swm.plugin.log.Logging;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class MysqlLoader extends UpdatableLoader {

    // World locking executor service
    private static final ScheduledExecutorService SERVICE = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
            .setNameFormat("SWM MySQL Lock Pool Thread #%1$d").build());

    private static final int CURRENT_DB_VERSION = 1;

    // Database version handling queries
    private static final String CREATE_VERSIONING_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS `database_version` (`id` INT NOT NULL AUTO_INCREMENT, " +
            "`version` INT(11), PRIMARY KEY(id));";
    private static final String INSERT_VERSION_QUERY = "INSERT INTO `database_version` (`id`, `version`) VALUES (1, ?) ON DUPLICATE KEY UPDATE `id` = ?;";
    private static final String GET_VERSION_QUERY = "SELECT `version` FROM `database_version` WHERE `id` = 1;";

    // v1 update query
    private static final String ALTER_LOCKED_COLUMN_QUERY = "ALTER TABLE `worlds` CHANGE COLUMN `locked` `locked` BIGINT NOT NULL DEFAULT 0;";

    // World handling queries
    private static final String CREATE_WORLDS_TABLE_QUERY = "CREATE TABLE IF NOT EXISTS `worlds` (`id` INT NOT NULL AUTO_INCREMENT, " +
            "`name` VARCHAR(255) UNIQUE, `world` MEDIUMBLOB, `locked` BIGINT, PRIMARY KEY(id));";
    private static final String SELECT_WORLD_QUERY = "SELECT `world`, `locked` FROM `worlds` WHERE `name` = ?;";
    private static final String UPDATE_WORLD_QUERY = "INSERT INTO `worlds` (`name`, `world`, `locked`) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE `world` = ?;";
    private static final String UPDATE_LOCK_QUERY = "UPDATE `worlds` SET `locked` = ? WHERE `name` = ?;";
    private static final String DELETE_WORLD_QUERY = "DELETE FROM `worlds` WHERE `name` = ?;";
    private static final String LIST_WORLDS_QUERY = "SELECT `name` FROM `worlds`;";

    private final Map<String, ScheduledFuture> lockedWorlds = new HashMap<>();
    private final HikariDataSource source;

    public MysqlLoader(DatasourcesConfig.MysqlConfig config) throws SQLException {
        HikariConfig hikariConfig = new HikariConfig();

        String sqlUrl = config.getSqlUrl();
        sqlUrl = sqlUrl.replace("{host}", config.getHost());
        sqlUrl = sqlUrl.replace("{port}", String.valueOf(config.getPort()));
        sqlUrl = sqlUrl.replace("{database}", config.getDatabase());
        sqlUrl = sqlUrl.replace("{usessl}", String.valueOf(config.isUsessl()));

        hikariConfig.setJdbcUrl(sqlUrl);
//        hikariConfig.setJdbcUrl("jdbc:mysql://" + config.getHost() + ":" + config.getPort() + "/" + config.getDatabase() + "?autoReconnect=true&allowMultiQueries=true&useSSL=" + config.isUsessl());
        hikariConfig.setUsername(config.getUsername());
        hikariConfig.setPassword(config.getPassword());

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

    @Override
    public void update() throws IOException, NewerDatabaseException {
        try (Connection con = source.getConnection()) {
            int version;

            try (PreparedStatement statement = con.prepareStatement(GET_VERSION_QUERY);
                 ResultSet set = statement.executeQuery()) {
                version = set.next() ? set.getInt(1) : -1;
            }

            if (version > CURRENT_DB_VERSION) {
                throw new NewerDatabaseException(CURRENT_DB_VERSION, version);
            }

            if (version < CURRENT_DB_VERSION) {
                Logging.warning("Your SWM MySQL database is outdated. The update process will start in 10 seconds.");
                Logging.warning("Note that this update might make your database incompatible with older SWM versions.");
                Logging.warning("Make sure no other servers with older SWM versions are using this database.");
                Logging.warning("Shut down the server to prevent your database from being updated.");

                try {
                    Thread.sleep(10000L);
                } catch (InterruptedException ignored) {

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
    public byte[] loadWorld(String worldName, boolean readOnly) throws UnknownWorldException, IOException, WorldInUseException {
        try (Connection con = source.getConnection();
            PreparedStatement statement = con.prepareStatement(SELECT_WORLD_QUERY)) {
            statement.setString(1, worldName);
            ResultSet set = statement.executeQuery();

            if (!set.next()) {
                throw new UnknownWorldException(worldName);
            }

            if (!readOnly) {
                long lockedMillis = set.getLong("locked");

                if (System.currentTimeMillis() - lockedMillis <= LoaderUtils.MAX_LOCK_TIME) {
                    throw new WorldInUseException(worldName);
                }

                updateLock(worldName, true);
            }

            return set.getBytes("world");
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    private void updateLock(String worldName, boolean forceSchedule) {
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(UPDATE_LOCK_QUERY)) {
            statement.setLong(1, System.currentTimeMillis());
            statement.setString(2, worldName);

            statement.executeUpdate();
        } catch (SQLException ex) {
            Logging.error("Failed to update the lock for world " + worldName + ":");
            ex.printStackTrace();
        }

        if (forceSchedule || lockedWorlds.containsKey(worldName)) { // Only schedule another update if the world is still on the map
            lockedWorlds.put(worldName, SERVICE.schedule(() -> updateLock(worldName, false), LoaderUtils.LOCK_INTERVAL, TimeUnit.MILLISECONDS));
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
    public void saveWorld(String worldName, byte[] serializedWorld, boolean lock) throws IOException {
        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(UPDATE_WORLD_QUERY)) {
            statement.setString(1, worldName);
            statement.setBytes(2, serializedWorld);
            statement.setBytes(3, serializedWorld);
            statement.executeUpdate();

            if (lock) {
                updateLock(worldName, true);
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void unlockWorld(String worldName) throws IOException, UnknownWorldException {
        ScheduledFuture future = lockedWorlds.remove(worldName);

        if (future != null) {
            future.cancel(false);
        }

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(UPDATE_LOCK_QUERY)) {
            statement.setLong(1, 0L);
            statement.setString(2, worldName);

            if (statement.executeUpdate() == 0) {
                throw new UnknownWorldException(worldName);
            }
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public boolean isWorldLocked(String worldName) throws IOException, UnknownWorldException {
        if (lockedWorlds.containsKey(worldName)) {
            return true;
        }

        try (Connection con = source.getConnection();
             PreparedStatement statement = con.prepareStatement(SELECT_WORLD_QUERY)) {
            statement.setString(1, worldName);
            ResultSet set = statement.executeQuery();

            if (!set.next()) {
                throw new UnknownWorldException(worldName);
            }

            return System.currentTimeMillis() - set.getLong("locked") <= LoaderUtils.MAX_LOCK_TIME;
        } catch (SQLException ex) {
            throw new IOException(ex);
        }
    }

    @Override
    public void deleteWorld(String worldName) throws IOException, UnknownWorldException {
        ScheduledFuture future = lockedWorlds.remove(worldName);

        if (future != null) {
            future.cancel(false);
        }

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
}
