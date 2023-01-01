package com.grinderwolf.swm.plugin.loaders.redis;

import com.infernalsuite.aswm.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.config.DatasourcesConfig;
import com.grinderwolf.swm.plugin.loaders.redis.util.StringByteCodec;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;

import java.io.IOException;
import java.util.List;

public class RedisLoader implements SlimeLoader {

    private static final String WORLD_DATA_PREFIX = "aswm_world_data_";
    private static final byte TRUE = 0x1;
    private static final byte FALSE = 0x0;

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
        return connection.get(WORLD_DATA_PREFIX + name) != null;
    }

    @Override
    public List<String> listWorlds() throws IOException {
        return connection.keys(WORLD_DATA_PREFIX + "*");
    }

    @Override
    public void saveWorld(String name, byte[] bytes) throws IOException {
        connection.set(WORLD_DATA_PREFIX + name, bytes);
    }

    @Override
    public void deleteWorld(String name) throws UnknownWorldException, IOException {
        boolean exists = this.worldExists(name);
        if (!exists) {
            throw new UnknownWorldException(name);
        }
        connection.del(WORLD_DATA_PREFIX + name);
    }
}