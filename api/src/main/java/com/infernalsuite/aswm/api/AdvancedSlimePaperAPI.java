package com.infernalsuite.aswm.api;

import com.infernalsuite.aswm.api.exceptions.*;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import net.kyori.adventure.util.Services;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Main class of the SWM API. From here, you can load
 * worlds and add them to the server's world list.
 * <br>
 * <b>All methods are allowed to be called asynchronously unless specifically stated in the javadoc</b>
 */
public interface AdvancedSlimePaperAPI {

    /**
     * Reads a world using a specified {@link SlimeLoader}.
     * <strong>This world won't be loaded into the server's world list.</strong>
     *
     * @param loader      {@link SlimeLoader} used to retrieve the world.
     * @param worldName   Name of the world.
     * @param readOnly    Whether read-only mode is enabled.
     * @param propertyMap A {@link SlimePropertyMap} object containing all the properties of the world.
     * @return A {@link SlimeWorld}, which is the in-memory representation of the world.
     * @throws UnknownWorldException   if the world cannot be found.
     * @throws IOException             if the world cannot be obtained from the specified data source.
     * @throws CorruptedWorldException if the world retrieved cannot be parsed into a {@link SlimeWorld} object.
     * @throws NewerFormatException    if the world uses a newer version of the SRF.
     */
    SlimeWorld readWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap) throws
            UnknownWorldException, IOException, CorruptedWorldException, NewerFormatException;

    /**
     * Gets a world which has already been loaded by ASWM.
     *
     * @param worldName the name of the world to get
     * @return the loaded world, or {@code null} if no loaded world matches the given name
     */
    SlimeWorld getLoadedWorld(String worldName);

    /**
     * Gets a list of worlds which have been loaded by ASWM.
     * Note: The returned list is immutable, and encompasses a view of the loaded worlds at the time of the method call.
     *
     * @return a list of worlds
     */
    List<SlimeWorld> getLoadedWorlds();

    /**
     * Generates a Minecraft World from a {@link SlimeWorld} and
     * adds it to the server's world list.
     * <br>
     * <b>This method must be called in sync with the Server Thread</b>
     *
     * @param world              {@link SlimeWorld} world to be added to the server's world list
     * @param callWorldLoadEvent Whether to call {@link org.bukkit.event.world.WorldLoadEvent}
     * @throws IllegalArgumentException if the world is already loaded
     * @return Returns a slime world representing a live minecraft world
     */
    SlimeWorld loadWorld(SlimeWorld world, boolean callWorldLoadEvent) throws IllegalArgumentException;

    /**
     * Checks if a {@link SlimeWorld} is loaded on the server.
     *
     * @param world The {@link SlimeWorld} to check.
     * @return {@code true} if the world is loaded, {@code false} otherwise.
     */
    boolean worldLoaded(SlimeWorld world);

    /**
     * Saves a {@link SlimeWorld} into the {@link SlimeLoader} obtained from {@link SlimeWorld#getLoader()}
     * <br>
     * It is suggested to use this instead of {@link World#save()}, as this method will block the current thread until the world is saved
     *
     * @param world The {@link SlimeWorld} to save.
     * @throws IOException if the world could not be saved.
     */
    void saveWorld(SlimeWorld world) throws IOException;

    /**
     * Migrates a {@link SlimeWorld} to another datasource.
     *
     * @param worldName     The name of the world to be migrated.
     * @param currentLoader The {@link SlimeLoader} of the data source where the world is currently stored in.
     * @param newLoader     The {@link SlimeLoader} of the data source where the world will be moved to.
     * @throws IOException                 if the world could not be migrated.
     * @throws WorldAlreadyExistsException if a world with the same name already exists inside the new data source.
     * @throws UnknownWorldException       if the world has been removed from the old data source.
     */
    void migrateWorld(String worldName, SlimeLoader currentLoader, SlimeLoader newLoader) throws IOException, WorldAlreadyExistsException, UnknownWorldException;

    /**
     * Creates an empty world.
     * <br>
     * <b>This method does not load the world, nor save it to a datasource.</b> Use {@link #loadWorld(SlimeWorld, boolean)}, and {@link #saveWorld(SlimeWorld)} for that.
     *
     * @param worldName   Name of the world.
     * @param readOnly    Whether read-only mode is enabled.
     * @param propertyMap A {@link SlimePropertyMap} object containing all the properties of the world.
     * @param loader      The {@link SlimeLoader} used to store the world when it gets loaded, or <code>null</code> if the world is temporary.
     * @return A {@link SlimeWorld}, which is the in-memory representation of the world.
     */
    SlimeWorld createEmptyWorld(String worldName, boolean readOnly, SlimePropertyMap propertyMap, @Nullable SlimeLoader loader);

    /**
     * Reads a vanilla world and converts it to SRF.
     * <br>
     * <b>This method does not load the world, nor save it to a datasource.</b> Use {@link #loadWorld(SlimeWorld, boolean)}, and {@link #saveWorld(SlimeWorld)} for that.
     *
     * @param worldDir  The directory where the world is.
     * @param worldName The name of the world.
     * @param loader    The {@link SlimeLoader} used to store the world when it gets loaded, or <code>null</code> if the world is temporary.
     * @return SlimeWorld to import
     * @throws InvalidWorldException       if the provided directory does not contain a valid world.
     * @throws WorldLoadedException        if the world is loaded on the server.
     * @throws WorldTooBigException        if the world is too big to be imported into the SRF.
     * @throws IOException                 if the world could not be read or stored.
     */
    SlimeWorld readVanillaWorld(File worldDir, String worldName, @Nullable SlimeLoader loader) throws InvalidWorldException, WorldLoadedException, WorldTooBigException, IOException, WorldAlreadyExistsException;

    /**
     * Gets the instance of the AdvancedSlimePaper API.
     *
     * @return the instance of the AdvancedSlimePaper API
     */
    static AdvancedSlimePaperAPI instance() {
        return Holder.INSTANCE;
    }

    @ApiStatus.Internal
    class Holder {
        private static final AdvancedSlimePaperAPI INSTANCE = Services.service(AdvancedSlimePaperAPI.class).orElseThrow();
    }
}
