package com.grinderwolf.swm.api.loaders;

import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;

import java.io.IOException;
import java.util.List;

/**
 * SlimeLoaders are in charge of loading worlds
 * from a data source, and also locking and
 * deleting them.
 */
public interface SlimeLoader {

    /**
     * Load a world's data file. In case {@code readOnly} is false,
     * the world automatically gets locked so no other server
     * can access it in write-mode.
     *
     * @param worldName The name of the world.
     * @param readOnly  If false, a {@link WorldInUseException} should be thrown when the world is locked.
     * @return The world's data file, contained inside a byte array.
     * @throws UnknownWorldException if the world cannot be found.
     * @throws WorldInUseException   if the world is locked
     * @throws IOException           if the world could not be obtained.
     */
    byte[] loadWorld(String worldName, boolean readOnly) throws UnknownWorldException, WorldInUseException, IOException;

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
     * @param lock            Whether or not the world should be relocked.
     * @throws IOException if the world could not be saved.
     */
    void saveWorld(String worldName, byte[] serializedWorld, boolean lock) throws IOException;

    /**
     * Unlocks a world.
     *
     * @param worldName The name of the world.
     * @throws UnknownWorldException if the world could not be found.
     * @throws IOException           if the world could not be locked/unlocked.
     */
    void unlockWorld(String worldName) throws UnknownWorldException, IOException;

    /**
     * Checks whether or not a world is locked.
     *
     * @param worldName The name of the world.
     * @return <code>true</code> if the world is locked, <code>false</code> otherwhise.
     * @throws UnknownWorldException if the world could not be found.
     * @throws IOException           if the world could not be obtained.
     */
    boolean isWorldLocked(String worldName) throws UnknownWorldException, IOException;

    /**
     * Deletes a world from the data source.
     *
     * @param worldName name of the world
     * @throws UnknownWorldException if the world could not be found.
     * @throws IOException           if the world could not be deleted.
     */
    void deleteWorld(String worldName) throws UnknownWorldException, IOException;

}
