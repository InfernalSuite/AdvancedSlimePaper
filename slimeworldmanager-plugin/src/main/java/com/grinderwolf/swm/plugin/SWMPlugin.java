package com.grinderwolf.swm.plugin;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.google.common.collect.ImmutableList;
import com.grinderwolf.swm.api.SlimePlugin;
import com.grinderwolf.swm.api.events.*;
import com.grinderwolf.swm.api.exceptions.*;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.SlimeNMS;
import com.grinderwolf.swm.nms.v1182.v1182SlimeNMS;
import com.grinderwolf.swm.nms.v119.v119SlimeNMS;
import com.grinderwolf.swm.nms.v1191.v1191SlimeNMS;
import com.grinderwolf.swm.nms.v1192.v1192SlimeNMS;
import com.grinderwolf.swm.nms.world.SlimeLoadedWorld;
import com.grinderwolf.swm.plugin.commands.CommandManager;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldData;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.loaders.slime.SlimeWorldReaderRegistry;
import com.grinderwolf.swm.plugin.log.Logging;
import com.grinderwolf.swm.plugin.upgrade.WorldUpgrader;
import com.grinderwolf.swm.plugin.world.WorldUnlocker;
import com.grinderwolf.swm.plugin.world.importer.WorldImporter;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.Difficulty;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static com.grinderwolf.swm.api.world.properties.SlimeProperties.*;

public class SWMPlugin extends JavaPlugin implements SlimePlugin, Listener {

    @Getter
    private SlimeNMS nms;

    private final Map<String, SlimeWorld> loadedWorlds = new ConcurrentHashMap<>();

    private v1182SlimeNMS v1182SlimeNMS;

    private static boolean isPaperMC = false;

    private static boolean checkIsPaper() {
        try {
            return Class.forName("com.destroystokyo.paper.PaperConfig") != null;
        } catch(ClassNotFoundException ex) {
            return false;
        }
    }
    
    private static final int BSTATS_ID = 5419;

    @Override
    public void onLoad() {
        isPaperMC = checkIsPaper();

        try {
            ConfigManager.initialize();
        } catch (NullPointerException | IOException ex) {
            Logging.error("Failed to load config files:");
            ex.printStackTrace();
            return;
        }

        LoaderUtils.registerLoaders();

        try {
            nms = getNMSBridge();
        } catch (InvalidVersionException ex) {
            Logging.error(ex.getMessage());
            return;
        }

        List<String> erroredWorlds = loadWorlds();

        // Default world override
        try {
            Properties props = new Properties();

            props.load(new FileInputStream("server.properties"));
            String defaultWorldName = props.getProperty("level-name");

            if (erroredWorlds.contains(defaultWorldName)) {
                Logging.error("Shutting down server, as the default world could not be loaded.");
                Bukkit.getServer().shutdown();
            } else if (getServer().getAllowNether() && erroredWorlds.contains(defaultWorldName + "_nether")) {
                Logging.error("Shutting down server, as the default nether world could not be loaded.");
                Bukkit.getServer().shutdown();
            } else if (getServer().getAllowEnd() && erroredWorlds.contains(defaultWorldName + "_the_end")) {
                Logging.error("Shutting down server, as the default end world could not be loaded.");
                Bukkit.getServer().shutdown();
            }

            SlimeWorld defaultWorld = loadedWorlds.get(defaultWorldName);
            SlimeWorld netherWorld = getServer().getAllowNether() ? loadedWorlds.get(defaultWorldName + "_nether") : null;
            SlimeWorld endWorld = getServer().getAllowEnd() ? loadedWorlds.get(defaultWorldName + "_the_end") : null;

            nms.setDefaultWorlds(defaultWorld, netherWorld, endWorld);
        } catch (IOException ex) {
            Logging.error("Failed to retrieve default world name:");
            ex.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        if (nms == null) {
            this.setEnabled(false);
            return;
        }
        
        Metrics metrics = new Metrics(this, BSTATS_ID);

        final CommandManager commandManager = new CommandManager();
        final PluginCommand swmCommand = getCommand("swm");
        swmCommand.setExecutor(commandManager);

        try {
            swmCommand.setTabCompleter(commandManager);
        } catch (Throwable throwable) {
            // For some versions that does not have TabComplete?
        }

        getServer().getPluginManager().registerEvents(new WorldUnlocker(), this);

        loadedWorlds.values().stream()
                .filter(slimeWorld -> Objects.isNull(Bukkit.getWorld(slimeWorld.getName())))
                .forEach(this::generateWorld);

        this.getServer().getPluginManager().registerEvents(this, this);

        //loadedWorlds.clear // - Commented out because not sure why this would be cleared. Needs checking
    }

    @Override
    public void onDisable() {
        Bukkit.getWorlds().stream()
                .map(world -> getNms().getSlimeWorld(world))
                .filter(Objects::nonNull)
                .filter((slimeWorld -> !slimeWorld.isReadOnly()))
                .map(w -> (SlimeLoadedWorld) w)
                .forEach(world -> {
                    try {
                        SlimeLoader loader = world.getLoader();
                        String name = world.getName();

                        loader.saveWorld(
                                name,
                                world.serialize().join(),
                                world.isLocked()
                        );

                        if (loader.isWorldLocked(name)) {
                            loader.unlockWorld(name);
                        }
                    } catch (IOException | UnknownWorldException e) {
                        e.printStackTrace();
                    }
                });
    }

    private SlimeNMS getNMSBridge() throws InvalidVersionException {
        String version = Bukkit.getServer().getClass().getPackage().getName();
        String nmsVersion = version.substring(version.lastIndexOf('.') + 1);

        int dataVersion = Bukkit.getUnsafe().getDataVersion();
        return switch (dataVersion) {
            case 2975 -> new v1182SlimeNMS(isPaperMC);
            case 3105 -> new v119SlimeNMS(isPaperMC);
            case 3117 -> new v1191SlimeNMS(isPaperMC);
            case 3120 -> new v1192SlimeNMS(isPaperMC);
            default -> throw new InvalidVersionException("" + dataVersion);
        };
    }

    private List<String> loadWorlds() {
        List<String> erroredWorlds = new ArrayList<>();
        WorldsConfig config = ConfigManager.getWorldConfig();

        for (Map.Entry<String, WorldData> entry : config.getWorlds().entrySet()) {
            String worldName = entry.getKey();
            WorldData worldData = entry.getValue();

            if (worldData.isLoadOnStartup()) {
                try {
                    SlimeLoader loader = getLoader(worldData.getDataSource());

                    if (loader == null) {
                        throw new IllegalArgumentException("invalid data source " + worldData.getDataSource() + "");
                    }

                    SlimePropertyMap propertyMap = worldData.toPropertyMap();
                    SlimeWorld world = loadWorld(loader, worldName, worldData.isReadOnly(), propertyMap);

                    loadedWorlds.put(worldName, world);
                } catch (IllegalArgumentException | UnknownWorldException | NewerFormatException | WorldInUseException | CorruptedWorldException | IOException ex) {
                    String message;

                    if (ex instanceof IllegalArgumentException) {
                        message = ex.getMessage();

                        ex.printStackTrace();
                    } else if (ex instanceof UnknownWorldException) {
                        message = "world does not exist, are you sure you've set the correct data source?";
                    } else if (ex instanceof NewerFormatException) {
                        message = "world is serialized in a newer Slime Format version (" + ex.getMessage() + ") that SWM does not understand.";
                    } else if (ex instanceof WorldInUseException) {
                        message = "world is in use! If you think this is a mistake, please wait some time and try again.";
                    } else if (ex instanceof CorruptedWorldException) {
                        message = "world seems to be corrupted.";
                    } else {
                        message = "";

                        ex.printStackTrace();
                    }

                    Logging.error("Failed to load world " + worldName + (message.isEmpty() ? "." : ": " + message));
                    erroredWorlds.add(worldName);
                }
            }
        }

        config.save();
        return erroredWorlds;
    }

    @Override
    public SlimeWorld loadWorld(SlimeLoader loader, String worldName, SlimeWorld.SlimeProperties properties) throws UnknownWorldException,
            IOException, CorruptedWorldException, NewerFormatException, WorldInUseException {
        Objects.requireNonNull(properties, "Properties cannot be null");

        return loadWorld(loader, worldName, properties.isReadOnly(), propertiesToMap(properties));
    }

    @Override
    public SlimeWorld loadWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap) throws UnknownWorldException, IOException,
            CorruptedWorldException, NewerFormatException, WorldInUseException {
        Objects.requireNonNull(loader, "Loader cannot be null");
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(propertyMap, "Properties cannot be null");

        long start = System.currentTimeMillis();

        Logging.info("Loading world " + worldName + ".");
        byte[] serializedWorld = loader.loadWorld(worldName, readOnly);
        SlimeLoadedWorld world;

        try {
            world = SlimeWorldReaderRegistry.readWorld(loader, worldName, serializedWorld, propertyMap, readOnly);

            if (world.getVersion() > nms.getWorldVersion()) {
                throw new NewerFormatException(world.getVersion());
            } else if (world.getVersion() < nms.getWorldVersion()) {
                WorldUpgrader.upgradeWorld(world);
            }
        } catch (Exception ex) {
            if (!readOnly) { // Unlock the world as we're not using it
                loader.unlockWorld(worldName);
            }

            throw ex;
        }

        Logging.info("World " + worldName + " loaded in " + (System.currentTimeMillis() - start) + "ms.");

        registerWorld(world);
        return world;
    }

    @Override
    public SlimeWorld getWorld(String worldName) {
        return loadedWorlds.get(worldName);
    }

    public List<SlimeWorld> getLoadedWorlds() {
        return ImmutableList.copyOf(loadedWorlds.values());
    }

    @Override
    public SlimeWorld createEmptyWorld(SlimeLoader loader, String worldName, SlimeWorld.SlimeProperties properties) throws WorldAlreadyExistsException, IOException {
        Objects.requireNonNull(properties, "Properties cannot be null");

        return createEmptyWorld(loader, worldName, properties.isReadOnly(), propertiesToMap(properties));
    }

    @Override
    public SlimeWorld createEmptyWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap) throws WorldAlreadyExistsException, IOException {
        Objects.requireNonNull(loader, "Loader cannot be null");
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(propertyMap, "Properties cannot be null");

        if (loader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        Logging.info("Creating empty world " + worldName + ".");
        long start = System.currentTimeMillis();
        SlimeLoadedWorld world = SWMPlugin.getInstance().getNms().createSlimeWorld(loader, worldName, new Long2ObjectOpenHashMap<>(), new CompoundTag("", new CompoundMap()), new ArrayList<>(), nms.getWorldVersion(), propertyMap, readOnly, !readOnly, new Long2ObjectOpenHashMap<>());
        loader.saveWorld(worldName, world.serialize().join(), !readOnly);

        Logging.info("World " + worldName + " created in " + (System.currentTimeMillis() - start) + "ms.");

        registerWorld(world);
        return world;
    }

    private SlimePropertyMap propertiesToMap(SlimeWorld.SlimeProperties properties) {
        SlimePropertyMap propertyMap = new SlimePropertyMap();

        propertyMap.setValue(SPAWN_X, (int) properties.getSpawnX());
        propertyMap.setValue(SPAWN_Y, (int) properties.getSpawnY());
        propertyMap.setValue(SPAWN_Z, (int) properties.getSpawnZ());
        propertyMap.setValue(DIFFICULTY, Difficulty.getByValue(properties.getDifficulty()).name());
        propertyMap.setValue(ALLOW_MONSTERS, properties.allowMonsters());
        propertyMap.setValue(ALLOW_ANIMALS, properties.allowAnimals());
        propertyMap.setValue(PVP, properties.isPvp());
        propertyMap.setValue(ENVIRONMENT, properties.getEnvironment());

        return propertyMap;
    }

    /**
     * Utility method to register a <b>loaded</b> {@link SlimeWorld} with the internal map (for {@link #getWorld} calls)
     *
     * @param world the world to register
     */
    private void registerWorld(SlimeWorld world) {
        this.loadedWorlds.put(world.getName(), world);
    }


    /**
     * Ensure worlds are removed from the loadedWorlds map when {@link Bukkit#unloadWorld} is called.
     */
    @EventHandler
    public void onBukkitWorldUnload(WorldUnloadEvent worldUnloadEvent) {
        loadedWorlds.remove(worldUnloadEvent.getWorld().getName());
    }

    @Override
    public void generateWorld(SlimeWorld slimeWorld) {
        Objects.requireNonNull(slimeWorld, "SlimeWorld cannot be null");

        if (!slimeWorld.isReadOnly() && !slimeWorld.isLocked()) {
            throw new IllegalArgumentException("This world cannot be loaded, as it has not been locked.");
        }

        var preEvent = new PreGenerateWorldEvent(slimeWorld);
        Bukkit.getPluginManager().callEvent(preEvent);

        if(preEvent.isCancelled()) {
            return;
        }

        nms.generateWorld(slimeWorld);
        var postEvent = new PostGenerateWorldEvent(slimeWorld);
        Bukkit.getPluginManager().callEvent(postEvent);
    }

    @Override
    public void migrateWorld(String worldName, SlimeLoader currentLoader, SlimeLoader newLoader) throws IOException,
            WorldInUseException, WorldAlreadyExistsException, UnknownWorldException {
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(currentLoader, "Current loader cannot be null");
        Objects.requireNonNull(newLoader, "New loader cannot be null");

        if (newLoader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        World bukkitWorld = Bukkit.getWorld(worldName);

        boolean leaveLock = false;

        if (bukkitWorld != null) {
            // Make sure the loaded world really is a SlimeWorld and not a normal Bukkit world
            SlimeLoadedWorld slimeWorld = (SlimeLoadedWorld) SWMPlugin.getInstance().getNms().getSlimeWorld(bukkitWorld);

            if (slimeWorld != null && currentLoader.equals(slimeWorld.getLoader())) {
                slimeWorld.setLoader(newLoader);

                if (!slimeWorld.isReadOnly()) { // We have to manually unlock the world so no WorldInUseException is thrown
                    currentLoader.unlockWorld(worldName);
                    leaveLock = true;
                }
            }
        }

        byte[] serializedWorld = currentLoader.loadWorld(worldName, false);

        newLoader.saveWorld(worldName, serializedWorld, leaveLock);
        currentLoader.deleteWorld(worldName);
    }

    @Override
    public SlimeLoader getLoader(String dataSource) {
        Objects.requireNonNull(dataSource, "Data source cannot be null");

        return LoaderUtils.getLoader(dataSource);
    }

    @Override
    public void registerLoader(String dataSource, SlimeLoader loader) {
        Objects.requireNonNull(dataSource, "Data source cannot be null");
        Objects.requireNonNull(loader, "Loader cannot be null");

        LoaderUtils.registerLoader(dataSource, loader);
    }

    @Override
    public void importWorld(File worldDir, String worldName, SlimeLoader loader) throws WorldAlreadyExistsException,
            InvalidWorldException, WorldLoadedException, WorldTooBigException, IOException {
        Objects.requireNonNull(worldDir, "World directory cannot be null");
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(loader, "Loader cannot be null");

        if (loader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        World bukkitWorld = Bukkit.getWorld(worldDir.getName());

        if (bukkitWorld != null && nms.getSlimeWorld(bukkitWorld) == null) {
            throw new WorldLoadedException(worldDir.getName());
        }

        SlimeLoadedWorld world = WorldImporter.readFromDirectory(worldDir);

        byte[] serializedWorld;

        try {
            serializedWorld = world.serialize().join();
        } catch (IndexOutOfBoundsException ex) {
            throw new WorldTooBigException(worldDir.getName());
        }

        loader.saveWorld(worldName, serializedWorld, false);
    }


    @Override
    public CompletableFuture<Optional<SlimeWorld>> asyncLoadWorld(SlimeLoader slimeLoader, String worldName, boolean readOnly, SlimePropertyMap slimePropertyMap) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var preEvent = new AsyncPreLoadWorldEvent(slimeLoader, worldName, readOnly, slimePropertyMap);
                Bukkit.getPluginManager().callEvent(preEvent);

                if(preEvent.isCancelled()) {
                    return Optional.empty();
                }

                var world = loadWorld(preEvent.getSlimeLoader(), preEvent.getWorldName(), preEvent.isReadOnly(), preEvent.getSlimePropertyMap());
                var postEvent = new AsyncPostLoadWorldEvent(world);
                Bukkit.getPluginManager().callEvent(postEvent);
                return Optional.ofNullable(postEvent.getSlimeWorld());
            } catch (UnknownWorldException | IOException | CorruptedWorldException | NewerFormatException | WorldInUseException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Optional<SlimeWorld>> asyncGetWorld(String worldName) {
        return CompletableFuture.supplyAsync(() -> {
            var preEvent = new AsyncPreGetWorldEvent(worldName);
            Bukkit.getPluginManager().callEvent(preEvent);

            if (preEvent.isCancelled()) {
                return Optional.empty();
            }

            var world = getWorld(preEvent.getWorldName());
            var postEvent = new AsyncPostGetWorldEvent(world);
            Bukkit.getPluginManager().callEvent(postEvent);
            return Optional.ofNullable(postEvent.getSlimeWorld());
        });
    }

    @Override
    public CompletableFuture<Optional<SlimeWorld>> asyncCreateEmptyWorld(SlimeLoader slimeLoader, String worldName, boolean readOnly, SlimePropertyMap slimePropertyMap) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                var preEvent = new AsyncPreCreateEmptyWorldEvent(slimeLoader, worldName, readOnly, slimePropertyMap);
                Bukkit.getPluginManager().callEvent(preEvent);

                if(preEvent.isCancelled()) {
                    return Optional.empty();
                }

                var world = createEmptyWorld(preEvent.getSlimeLoader(), preEvent.getWorldName(), preEvent.isReadOnly(), preEvent.getSlimePropertyMap());
                var postEvent = new AsyncPostCreateEmptyWorldEvent(world);
                Bukkit.getPluginManager().callEvent(postEvent);
                return Optional.ofNullable(postEvent.getSlimeWorld());
            } catch (WorldAlreadyExistsException | IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> asyncMigrateWorld(String worldName, SlimeLoader currentLoader, SlimeLoader newLoader) {
        return CompletableFuture.runAsync(() -> {
            try {
                var preEvent = new AsyncPreMigrateWorldEvent(worldName, currentLoader, newLoader);
                Bukkit.getPluginManager().callEvent(preEvent);

                if(preEvent.isCancelled()) {
                    return;
                }

                migrateWorld(preEvent.getWorldName(), preEvent.getCurrentLoader(), preEvent.getNewLoader());
                var postEvent = new AsyncPostMigrateWorldEvent(preEvent.getWorldName(), preEvent.getCurrentLoader(), preEvent.getNewLoader());
                Bukkit.getPluginManager().callEvent(postEvent);
            } catch (IOException | WorldInUseException | WorldAlreadyExistsException | UnknownWorldException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Override
    public CompletableFuture<Void> asyncImportWorld(File worldDir, String worldName, SlimeLoader slimeLoader) {
        return CompletableFuture.runAsync(() -> {
            try {
                var preEvent = new AsyncPreImportWorldEvent(worldDir, worldName, slimeLoader);
                Bukkit.getPluginManager().callEvent(preEvent);

                if(preEvent.isCancelled()) {
                    return;
                }

                importWorld(preEvent.getWorldDir(), preEvent.getWorldName(), preEvent.getSlimeLoader());
                var postEvent = new AsyncPostImportWorldEvent(preEvent.getWorldDir(), preEvent.getWorldName(), preEvent.getSlimeLoader());
                Bukkit.getPluginManager().callEvent(postEvent);
            } catch (WorldAlreadyExistsException | InvalidWorldException | WorldLoadedException | WorldTooBigException | IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    public static boolean isPaperMC() {
        return isPaperMC;
    }

    public static SWMPlugin getInstance() {
        return SWMPlugin.getPlugin(SWMPlugin.class);
    }
}
