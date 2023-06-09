package com.grinderwolf.swm.plugin.loaders.redis;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.exceptions.WorldLockedException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.config.DatasourcesConfig;
import com.grinderwolf.swm.plugin.loaders.redis.util.StringByteCodec;
import io.lettuce.core.*;
import io.lettuce.core.api.sync.RedisCommands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RedisLoader implements SlimeLoader {

    // World locking executor service
    private static final ScheduledExecutorService SERVICE = Executors.newScheduledThreadPool(2, new ThreadFactoryBuilder()
            .setNameFormat("SWM Redis Lock Pool Thread #%1$d").build());

    private static final String WORLD_DATA_PREFIX = "aswm:world:data:";
    private static final String WORLD_LOCK_PREFIX = "aswm:world:lock:";
    private static final String WORLD_LIST_PREFIX = "aswm:world:list";
    private static final byte TRUE = 0x1;

    private final Map<String, ScheduledFuture<?>> lockedWorlds = new HashMap<>();
    private final RedisCommands<String, byte[]> connection;

    public RedisLoader(DatasourcesConfig.RedisConfig config) {
        this.connection = RedisClient
            .create(config.getUri())
            .connect(StringByteCodec.INSTANCE)
            .sync();
    }

    @Override
    public byte[] loadWorld(String name) throws UnknownWorldException, IOException {
        byte[] data = connection.get(WORLD_DATA_PREFIX + name);
        if (data == null) {
            throw new UnknownWorldException(name);
        }
        return data;
    }

    @Override
    public boolean worldExists(String name) throws IOException {
        return connection.exists(WORLD_DATA_PREFIX + name) == 1;
    }

    @Override
    public List<String> listWorlds() throws IOException {
        return connection.smembers(WORLD_LIST_PREFIX)
                .stream()
                .map(bytes -> new String(bytes, StandardCharsets.UTF_8))
                .collect(Collectors.toList()); // We can't use .toList because this needs to be mutable.
    }

    @Override
    public void saveWorld(String worldName, byte[] bytes) throws IOException {
        connection.set(WORLD_DATA_PREFIX + worldName, bytes);

        // Also add to the world list set. We can't do this in one atomic operation (mset) because it's a set add
        connection.sadd(WORLD_LIST_PREFIX, worldName.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void deleteWorld(String worldName) throws UnknownWorldException, IOException {
        ScheduledFuture<?> future = lockedWorlds.remove(worldName);

        if (future != null) {
            future.cancel(false);
        }

        long deletedCount = connection.del(WORLD_DATA_PREFIX + worldName, WORLD_LOCK_PREFIX + worldName);

        // We're checking equal to zero, because the lock key doesn't have to exist
        if (deletedCount == 0) {
            throw new UnknownWorldException(worldName);
        }

        // Remove the world from the world list set
        connection.srem(WORLD_LIST_PREFIX, worldName.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void acquireLock(String worldName) throws UnknownWorldException, WorldLockedException, IOException {
        boolean wasSet = connection.setnx(WORLD_LOCK_PREFIX + worldName, new byte[]{TRUE});
        if (!wasSet) {
            // The key already exists, so the setnx returned 0 (false)
            throw new WorldLockedException(worldName);
        }

        // Set the expiry on the key
        updateLock(worldName, true);
    }

    private void updateLock(String worldName, boolean forceSchedule) throws UnknownWorldException {
        // Set the key to expire in LoaderUtils.MAX_LOCK_TIME ms. Using pexpire not expire because milliseconds.
        boolean wasSet = connection.pexpire(WORLD_LOCK_PREFIX + worldName, LoaderUtils.MAX_LOCK_TIME);
        if (!wasSet) {
            throw new UnknownWorldException(worldName);
        }

        if (forceSchedule || lockedWorlds.containsKey(worldName)) { // Only schedule another update if the world is still on the map
            lockedWorlds.put(worldName, SERVICE.schedule(() -> {
                try {
                    updateLock(worldName, false);
                } catch (UnknownWorldException e) {
                    // This is the case where a schedule tries to update a lock after the world has been deleted
                    // This could be possible if a different server were to run the deletion, or if redis
                    // was to purge those keys for memory reasons.

                    // This is not a problem, so we can just ignore it.
                }
            }, LoaderUtils.LOCK_INTERVAL, TimeUnit.MILLISECONDS));
        }
    }

    @Override
    public boolean isWorldLocked(String worldName) throws UnknownWorldException, IOException {
        if (lockedWorlds.containsKey(worldName)) {
            return true;
        }

        return connection.exists(WORLD_LOCK_PREFIX + worldName) == 1;
    }

    @Override
    public void unlockWorld(String worldName) throws UnknownWorldException, IOException {
        ScheduledFuture<?> future = lockedWorlds.remove(worldName);

        if (future != null) {
            future.cancel(false);
        }

        connection.del(WORLD_LOCK_PREFIX + worldName);
    }
}