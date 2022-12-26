package com.grinderwolf.swm.plugin.loaders.redis;

import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.config.DatasourcesConfig;
import com.grinderwolf.swm.plugin.loaders.redis.util.StringByteCodec;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.sync.RedisCommands;

import java.io.IOException;
import java.util.List;

public class RedisLoader implements SlimeLoader {

    private static final String WORLD_DATA_PREFIX = "aswm_world_data_";
    private static final String WORLD_LOCK_PREFIX = "aswm_world_lock_";
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
    public byte[] loadWorld(String name, boolean readOnly) throws UnknownWorldException, WorldInUseException, IOException {
        if (!readOnly) {
            byte[] lock = connection.get(WORLD_LOCK_PREFIX + name);
            if (lock == null) {
                throw new UnknownWorldException(name);
            }
            if (lock[0] == TRUE) {
                throw new WorldInUseException(name);
            }
        }

        byte[] data = connection.get(WORLD_DATA_PREFIX + name);
        if (data == null) {
            throw new UnknownWorldException(name);
        }
        return data;
    }

    @Override
    public boolean worldExists(String name) throws IOException {
        return connection.get(WORLD_LOCK_PREFIX + name) != null;
    }

    @Override
    public List<String> listWorlds() throws IOException {
        return connection.keys(WORLD_LOCK_PREFIX + "*");
    }

    @Override
    public void saveWorld(String name, byte[] bytes, boolean lock) throws IOException {
        connection.set(WORLD_DATA_PREFIX + name, bytes);
        connection.set(WORLD_LOCK_PREFIX + name, new byte[]{lock ? TRUE : FALSE});
    }

    @Override
    public void unlockWorld(String name) throws UnknownWorldException, IOException {
        boolean exists = this.worldExists(name);
        if (!exists) {
            throw new UnknownWorldException(name);
        }
        connection.set(WORLD_LOCK_PREFIX + name, new byte[]{(byte) FALSE});
    }

    @Override
    public boolean isWorldLocked(String name) throws UnknownWorldException, IOException {
        byte[] response = connection.get(WORLD_LOCK_PREFIX + name);
        if (response == null) {
            throw new UnknownWorldException(name);
        }
        return response[0] == TRUE;
    }

    @Override
    public void deleteWorld(String name) throws UnknownWorldException, IOException {
        boolean exists = this.worldExists(name);
        if (!exists) {
            throw new UnknownWorldException(name);
        }
        connection.del(WORLD_DATA_PREFIX + name, WORLD_LOCK_PREFIX + name);
    }
}