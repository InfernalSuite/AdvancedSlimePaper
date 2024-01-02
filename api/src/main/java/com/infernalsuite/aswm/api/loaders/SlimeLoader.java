package com.infernalsuite.aswm.api.loaders;

import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.exceptions.WorldLockedException;

import java.io.IOException;
import java.util.List;

/**
 * SlimeLoaders are in charge of loading worlds
 * from a data source, and also locking and
 * deleting them.
 */
public interface SlimeLoader {

    /**
     * Load a world's data file.
     *
     * @param worldName The name of the world.
     * @return The world's data file, contained inside a byte array.
     * @throws UnknownWorldException if the world cannot be found.
     * @throws IOException           if the world could not be obtained.
     */
    byte[] loadWorld(String worldName) throws UnknownWorldException, IOException;

    /**
     * Checks whether or not a world exists
     * inside the data source.
     *
     * @param worldName The name of the world.
     * @return <code>true</code> if the world exists inside the data source, <code>false</code> otherwhise.
     * @throws IOException if the world could not be obtained.
     */
    boolean worldExists(String worldName) throws IOException;

    /**
     * Returns the current saved world names.
     *
     * @return a list containing all the world names
     * @throws IOException if the list could not be obtained
     */
    List<String> listWorlds() throws IOException;

    /**
     * Saves the world's data file. This method will also
     * lock the world, in case it's not locked already.
     *
     * @param worldName       The name of the world.
     * @param serializedWorld The world's data file, contained inside a byte array.
     * @throws IOException if the world could not be saved.
     */
    void saveWorld(String worldName, byte[] serializedWorld) throws IOException;

    /**
     * Deletes a world from the data source.
     *
     * @param worldName name of the world
     * @throws UnknownWorldException if the world could not be found.
     * @throws IOException           if the world could not be deleted.
     */
    void deleteWorld(String worldName) throws UnknownWorldException, IOException;

    /**
     * Attempts to lock the world.
     *
     * @param worldName name of the world
     * @throws UnknownWorldException If the world could not be found
     * @throws WorldLockedException  If the world is already locked
     * @throws IOException If the world could not be locked
     */
    @Deprecated(forRemoval = true)
    void acquireLock(String worldName) throws UnknownWorldException, WorldLockedException, IOException;

    /**
     * Checks whether or not a world is locked.
     *
     * @param worldName The name of the world.
     * @return <code>true</code> if the world is locked, <code>false</code> otherwhise.
     * @throws UnknownWorldException if the world could not be found.
     * @throws IOException           if the world could not be obtained.
     */
    @Deprecated(forRemoval = true)
    boolean isWorldLocked(String worldName) throws UnknownWorldException, IOException;

    /**
     * Attempts to unlock the world.
     *
     * @param worldName name of the world
     * @throws UnknownWorldException If the world could not be found
     * @throws IOException If the world could not be unlocked
     */
    @Deprecated(forRemoval = true)
    void unlockWorld(String worldName) throws UnknownWorldException, IOException;

}
