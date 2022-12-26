package com.grinderwolf.swm.api;

import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Main class of the SWM API. From here, you can load
 * worlds and add them to the server's world list, and
 * also add your own implementations of the {@link SlimeLoader}
 * interface, to load and store worlds from other data sources.
 */
public interface SlimePlugin {

    /**
     * Loads a world using a specificied {@link SlimeLoader}.
     * This world can then be added to the server's world
     * list by using the {@link #generateWorld(SlimeWorld)} method.
     *
     * @param loader {@link SlimeLoader} used to retrieve the world.
     * @param worldName Name of the world.
     * @param properties Properties of the world contained within a {@link SlimeWorld.SlimeProperties} object.
     *
     * @return A {@link SlimeWorld}, which is the in-memory representation of the world.
     *
     * @throws UnknownWorldException if the world cannot be found.
     * @throws IOException if the world cannot be obtained from the speficied data source.
     * @throws CorruptedWorldException if the world retrieved cannot be parsed into a {@link SlimeWorld} object.
     * @throws NewerFormatException if the world uses a newer version of the SRF.
     * @throws WorldInUseException if the world is already being used on another server when trying to open it without read-only mode enabled.
     *
     * @deprecated see {@link #loadWorld(SlimeLoader, String, boolean, SlimePropertyMap)}
     */
    @Deprecated
    SlimeWorld loadWorld(SlimeLoader loader, String worldName, SlimeWorld.SlimeProperties properties) throws
            UnknownWorldException, IOException, CorruptedWorldException, NewerFormatException, WorldInUseException;

    /**
     * Loads a world using a specificied {@link SlimeLoader}.
     * This world can then be added to the server's world
     * list by using the {@link #generateWorld(SlimeWorld)} method.
     *
     * @param loader {@link SlimeLoader} used to retrieve the world.
     * @param worldName Name of the world.
     * @param readOnly Whether or not read-only mode is enabled.
     * @param propertyMap A {@link SlimePropertyMap} object containing all the properties of the world.
     *
     * @return A {@link SlimeWorld}, which is the in-memory representation of the world.
     *
     * @throws UnknownWorldException if the world cannot be found.
     * @throws IOException if the world cannot be obtained from the speficied data source.
     * @throws CorruptedWorldException if the world retrieved cannot be parsed into a {@link SlimeWorld} object.
     * @throws NewerFormatException if the world uses a newer version of the SRF.
     * @throws WorldInUseException if the world is already being used on another server when trying to open it without read-only mode enabled.
     */
    SlimeWorld loadWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap) throws
            UnknownWorldException, IOException, CorruptedWorldException, NewerFormatException, WorldInUseException;

    /**
     * Gets a world which has already been loaded by ASWM.
     *
     * @param worldName the name of the world to get
     * @return the loaded world, or {@code null} if no loaded world matches the given name
     */
    SlimeWorld getWorld(String worldName);

    /**
     * Gets a list of worlds which have been loaded by ASWM.
     *
     * @apiNote the returned list is immutable, and encompasses a view of the loaded worlds at the time of the method call.
     * @return a list of worlds
     */
    List<SlimeWorld> getLoadedWorlds();

    /**
     * Creates an empty world and stores it using a specified
     * {@link SlimeLoader}. This world can then be added to
     * the server's world list by using the {@link #generateWorld(SlimeWorld)} method.
     *
     * @param loader {@link SlimeLoader} used to store the world.
     * @param worldName Name of the world.
     * @param properties Properties of the world contained within a {@link SlimeWorld.SlimeProperties} object.
     *
     * @return A {@link SlimeWorld}, which is the in-memory representation of the world.
     *
     * @throws WorldAlreadyExistsException if the provided data source already contains a world with the same name.
     * @throws IOException if the world could not be stored.
     *
     * @deprecated see {@link #createEmptyWorld(SlimeLoader, String, boolean, SlimePropertyMap)}
     */
    @Deprecated
    SlimeWorld createEmptyWorld(SlimeLoader loader, String worldName, SlimeWorld.SlimeProperties properties) throws WorldAlreadyExistsException, IOException;

    /**
     * Creates an empty world and stores it using a specified
     * {@link SlimeLoader}. This world can then be added to
     * the server's world list by using the {@link #generateWorld(SlimeWorld)} method.
     *
     * @param loader {@link SlimeLoader} used to store the world.
     * @param worldName Name of the world.
     * @param readOnly Whether or not read-only mode is enabled.
     * @param propertyMap A {@link SlimePropertyMap} object containing all the properties of the world.
     *
     * @return A {@link SlimeWorld}, which is the in-memory representation of the world.
     *
     * @throws WorldAlreadyExistsException if the provided data source already contains a world with the same name.
     * @throws IOException if the world could not be stored.
     */
    SlimeWorld createEmptyWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap) throws WorldAlreadyExistsException, IOException;

    /**
     * Generates a Minecraft World from a {@link SlimeWorld} and
     * adds it to the server's world list.
     *
     * @param world {@link SlimeWorld} world to be added to the server's world list
     */
    void generateWorld(SlimeWorld world);

    /**
     * Migrates a {@link SlimeWorld} to another datasource.
     *
     * @param worldName The name of the world to be migrated.
     * @param currentLoader The {@link SlimeLoader} of the data source where the world is currently stored in.
     * @param newLoader The {@link SlimeLoader} of the data source where the world will be moved to.
     *
     * @throws IOException if the world could not be migrated.
     * @throws WorldInUseException if the world is being used on a server.
     * @throws WorldAlreadyExistsException if a world with the same name already exists inside the new data source.
     * @throws UnknownWorldException if the world has been removed from the old data source.
     */
    void migrateWorld(String worldName, SlimeLoader currentLoader, SlimeLoader newLoader) throws IOException, WorldInUseException, WorldAlreadyExistsException, UnknownWorldException;

    /**
     * Returns the {@link SlimeLoader} that is able to
     * read and store worlds from a specified data source.
     *
     * @param dataSource {@link String} containing the data source.
     *
     * @return The {@link SlimeLoader} capable of reading and writing to the data source.
     */
    SlimeLoader getLoader(String dataSource);

    /**
     * Registers a custom {@link SlimeLoader}. This loader can
     * then be used by Slime World Manager to load and store worlds.
     *
     * @param dataSource The data source this loader is capable of reading and writing to.
     * @param loader The {@link SlimeLoader} that is going to be registered.
     */
    void registerLoader(String dataSource, SlimeLoader loader);

    /**
     * Imports a world into the SRF and saves it in a data source.
     *
     * @param worldDir The directory where the world is.
     * @param worldName The name of the world.
     * @param loader The {@link SlimeLoader} that will be used to store the world.
     *
     * @throws WorldAlreadyExistsException if the data source already contains a world with the same name.
     * @throws InvalidWorldException if the provided directory does not contain a valid world.
     * @throws WorldLoadedException if the world is loaded on the server.
     * @throws WorldTooBigException if the world is too big to be imported into the SRF.
     * @throws IOException if the world could not be read or stored.
     */
    void importWorld(File worldDir, String worldName, SlimeLoader loader) throws WorldAlreadyExistsException,
            InvalidWorldException, WorldLoadedException, WorldTooBigException, IOException;

    CompletableFuture<Optional<SlimeWorld>> asyncLoadWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap);

    CompletableFuture<Optional<SlimeWorld>> asyncGetWorld(String worldName);

    CompletableFuture<Optional<SlimeWorld>> asyncCreateEmptyWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap);

    CompletableFuture<Void> asyncMigrateWorld(String worldName, SlimeLoader currentLoader, SlimeLoader newLoader);

    CompletableFuture<Void> asyncImportWorld(File worldDir, String worldName, SlimeLoader loader);
}
