package com.grinderwolf.swm.clsm;

import java.util.concurrent.CompletableFuture;

/**
 * This class serves as a bridge between the SWM and the Minecraft server.
 *
 * As plugins are loaded using a different ClassLoader, their code cannot
 * be accessed from a NMS method. Because of this, it's impossible to make
 * any calls to any method when rewriting the bytecode of a NMS class.
 *
 * As a workaround, this bridge simply calls a method of the {@link CLSMBridge} interface,
 * which is implemented by the SWM plugin when loaded.
 */
public class ClassModifier {

    private static CLSMBridge customLoader;

    public static Object getChunkTask(final Object scheduler, final Object world, final int chunkX, final int chunkZ, final Object chunkHolder, final Object priority, final Object status) {
        if (customLoader == null || !isCustomWorld(world)) {
            return null;
        }

        return customLoader.getChunkTask(scheduler, world, chunkX, chunkZ, chunkHolder, priority, status);
    }

    public static CompletableFuture getFutureChunk(Object world, int x, int z) {
        if (customLoader == null || !isCustomWorld(world)) {
            return null;
        }

        return CompletableFuture.supplyAsync(() ->
            customLoader.getChunk(world, x, z)
        );
    }

    public static boolean saveChunk(Object world, Object chunkAccess) {
        return customLoader != null && customLoader.saveChunk(world, chunkAccess);
    }

    public static Object loadEntities(Object storage, Object coords) {
        if (customLoader == null) {
            return null;
        }

        return customLoader.loadEntities(storage, coords);
    }

    public static boolean storeEntities(Object storage, Object entities) {
        if (customLoader == null) {
            return false;
        }

        return customLoader.storeEntities(storage, entities);
    }

    public static boolean flushEntities(Object storage) {
        if (customLoader == null) {
            return false;
        }

        return customLoader.flushEntities(storage);
    }

    public static boolean isCustomWorld(Object world) {
        return customLoader != null && customLoader.isCustomWorld(world);
    }

    public static void setLoader(CLSMBridge loader) {
        customLoader = loader;
    }

    public static Object injectCustomWorlds() {
        return customLoader.injectCustomWorlds();
    }
}
