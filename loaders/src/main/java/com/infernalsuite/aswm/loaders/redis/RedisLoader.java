package com.infernalsuite.aswm.loaders.redis;

import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.loaders.redis.util.StringByteCodec;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

public class RedisLoader implements SlimeLoader {

    private static final String WORLD_DATA_PREFIX = "aswm:world:data:";
    private static final String WORLD_LIST_PREFIX = "aswm:world:list";

    private final RedisCommands<String, byte[]> connection;

    public RedisLoader(String uri) {
        this.connection = RedisClient
            .create(uri)
            .connect(StringByteCodec.INSTANCE)
            .sync();
    }

    @Override
    public byte[] readWorld(String name) throws UnknownWorldException, IOException {
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
        long deletedCount = connection.del(WORLD_DATA_PREFIX + worldName);

        // We're checking equal to zero, because the lock key doesn't have to exist
        if (deletedCount == 0) {
            throw new UnknownWorldException(worldName);
        }

        // Remove the world from the world list set
        connection.srem(WORLD_LIST_PREFIX, worldName.getBytes(StandardCharsets.UTF_8));
    }
}