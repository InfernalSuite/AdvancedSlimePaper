package com.grinderwolf.swm.clsm;

import lombok.NonNull;

public interface CLSMBridge {

    /**
     * Returns an CompletableFuture that is Either.left(chunk).
     * This can be completed async if needed.
     */
    default Object getChunk(Object world, int x, int z) {
        return null;
    }

    /**
     * Called when a chunk should be saved.
     *
     * @param world level
     * @param chunkAccess chunk
     * @return return true to run custom saving behavior, false to default to normal behavior
     */
    default boolean saveChunk(Object world, Object chunkAccess) {
        return false;
    }


    /**
     * Called when an entity chunk should be saved.
     *
     * @return return true to run custom saving behavior, false to default to normal behavior
     */
    default boolean storeEntities(Object storage, Object entityList) {
        return false;
    }

    /**
     * Called when an entity chunk should be populated
     *
     * @return return entity list to run custom, null to default to normal behavior
     */
    default Object loadEntities(Object storage, Object chunkCoords) {
        return null;
    }

    /**
     * Called when an entity chunk should be flushed
     *
     * @return return entity list to run custom, null to default to normal behavior
     */
    default boolean flushEntities(Object storage) {
        return false;
    }

    /**
     * Returns if the world is a custom ASWM world.
     *
     * @param world world instance
     * @return is a world or not
     */
    boolean isCustomWorld(Object world);

    /**
     * Called when the initial dimensions are being inserted into the game.
     * This overrides the dimensions() method, and the return value of this method
     * will replace the value of dimensions(). Return null to return the normal dimensions (overworld, nether, the end).
     *
     * In the case of injecting, it should return an EMPTY MappedRegistry and then inject any custom worlds.
     *
     * @return EMPTY MappedRegistry or null
     */
    Object injectCustomWorlds();

    @NonNull
    Object getChunkTask(Object scheduler, Object world, int chunkX, int chunkZ, Object chunkHolder, Object priority, Object status);
}
