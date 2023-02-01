package com.grinderwolf.swm.plugin.loaders.redis;

import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.exceptions.WorldLockedException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.config.DatasourcesConfig;
import com.grinderwolf.swm.plugin.loaders.redis.util.StringByteCodec;
import io.lettuce.core.*;
import io.lettuce.core.api.sync.RedisCommands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RedisLoader implements SlimeLoader {

    private static final String WORLD_DATA_PREFIX = "aswm:world:data:";
    private static final String WORLD_LOCK_PREFIX = "aswm:world:lock:";
    private static final String WORLD_LIST_PREFIX = "aswm:world:list";
    private static final byte TRUE = 0x1;

    public RedisLoader(DatasourcesConfig.RedisConfig config) {
        this.connection = RedisClient
            .create(config.getUri())
            .connect(StringByteCodec.INSTANCE)
            .sync();
    }

    private final RedisCommands<String, byte[]> connection;

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
                .toList();
    }

    @Override
    public void saveWorld(String name, byte[] bytes) throws IOException {
        connection.set(WORLD_DATA_PREFIX + name, bytes);

        // Also add to the world list set. We can't do this in one atomic operation (mset) because it's a set add
        connection.sadd(WORLD_LIST_PREFIX, name.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void deleteWorld(String name) throws UnknownWorldException, IOException {
        long deletedCount = connection.del(WORLD_DATA_PREFIX + name, WORLD_LOCK_PREFIX + name);

        // We're checking equal to zero, because the lock key doesn't have to exist
        if (deletedCount == 0) {
            throw new UnknownWorldException(name);
        }

        // Remove the world from the world list set
        connection.srem(WORLD_LIST_PREFIX, name.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void acquireLock(String worldName) throws UnknownWorldException, WorldLockedException, IOException {
        boolean wasSet = connection.setnx(WORLD_LOCK_PREFIX + worldName, new byte[]{TRUE});
        if (!wasSet) {
            // The key already exists, so the setnx returned 0 (false)
            throw new WorldLockedException(worldName);
        }
    }

    @Override
    public boolean isWorldLocked(String worldName) throws UnknownWorldException, IOException {
        return connection.exists(WORLD_LOCK_PREFIX + worldName) == 1;
    }

    @Override
    public void unlockWorld(String worldName) throws UnknownWorldException, IOException {
        connection.del(WORLD_LOCK_PREFIX + worldName);
    }
}