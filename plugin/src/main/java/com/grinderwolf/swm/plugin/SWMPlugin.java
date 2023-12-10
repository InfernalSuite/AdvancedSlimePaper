package com.grinderwolf.swm.plugin;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.google.common.collect.ImmutableList;
import com.grinderwolf.swm.plugin.commands.CommandManager;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldData;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import com.grinderwolf.swm.plugin.listeners.WorldUnlocker;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.log.Logging;
import com.infernalsuite.aswm.api.SlimeNMSBridge;
import com.infernalsuite.aswm.api.SlimePlugin;
import com.infernalsuite.aswm.api.events.LoadSlimeWorldEvent;
import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.InvalidWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.aswm.api.exceptions.WorldLoadedException;
import com.infernalsuite.aswm.api.exceptions.WorldLockedException;
import com.infernalsuite.aswm.api.exceptions.WorldTooBigException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.serialization.anvil.AnvilWorldReader;
import com.infernalsuite.aswm.serialization.slime.SlimeSerializer;
import com.infernalsuite.aswm.serialization.slime.reader.SlimeWorldReaderRegistry;
import com.infernalsuite.aswm.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.SlimeWorldInstance;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class SWMPlugin extends JavaPlugin implements SlimePlugin, Listener {

    private static final SlimeNMSBridge BRIDGE_INSTANCE = SlimeNMSBridge.instance();

    private final Map<String, SlimeWorld> loadedWorlds = new ConcurrentHashMap<>();

    private static boolean isPaperMC = false;

    private static boolean checkIsPaper() {
        try {
            return Class.forName("com.destroystokyo.paper.PaperConfig") != null;
        } catch (ClassNotFoundException ex) {
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

            BRIDGE_INSTANCE.setDefaultWorlds(defaultWorld, netherWorld, endWorld);
        } catch (IOException ex) {
            Logging.error("Failed to retrieve default world name:");
            ex.printStackTrace();
        }
    }

    @Override
    public void onEnable() {
        if (BRIDGE_INSTANCE == null) {
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

        loadedWorlds.values().stream()
                .filter(slimeWorld -> Objects.isNull(Bukkit.getWorld(slimeWorld.getName())))
                .forEach(slimeWorld -> {
                    try {
                        loadWorld(slimeWorld, true);
                    } catch (UnknownWorldException | WorldLockedException | IOException exception) {
                        Logging.error("Failed to load world: " + slimeWorld.getName());
                        exception.printStackTrace();
                    }
                });

        this.getServer().getPluginManager().registerEvents(this, this);
        this.getServer().getPluginManager().registerEvents(new WorldUnlocker(), this);
        //loadedWorlds.clear // - Commented out because not sure why this would be cleared. Needs checking
    }

    @Override
    public void onDisable() {
//        Bukkit.getWorlds().stream()
//                .map(world -> bridge.getSlimeWorld(world))
//                .filter(Objects::nonNull)
//                .filter((slimeWorld -> !slimeWorld.isReadOnly()))
//                .map(w -> (SlimeLoadedWorld) w)
//                .forEach(world -> {
//                    try {
//                        SlimeLoader loader = world.getLoader();
//                        String name = world.getName();
//
//                        loader.saveWorld(
//                                name,
//                                world.serialize().join(),
//                                world.isLocked()
//                        );
//
//                        if (loader.isWorldLocked(name)) {
//                            loader.unlockWorld(name);
//                        }
//                    } catch (IOException | UnknownWorldException e) {
//                        e.printStackTrace();
//                    }
//                });
    }

//    private SlimeNMS getNMSBridge() throws InvalidVersionException {
//        String version = Bukkit.getServer().getClass().getPackage().getName();
//        String nmsVersion = version.substring(version.lastIndexOf('.') + 1);
//
//        int dataVersion = Bukkit.getUnsafe().getDataVersion();
//        return switch (dataVersion) {
//            case 2975 -> new v1182SlimeNMS(isPaperMC);
//            case 3105 -> new v119SlimeNMS(isPaperMC);
//            case 3117 -> new v1191SlimeNMS(isPaperMC);
//            case 3120 -> new v1192SlimeNMS(isPaperMC);
//            default -> throw new InvalidVersionException("" + dataVersion);
//        };
//    }

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
                } catch (IllegalArgumentException | UnknownWorldException | NewerFormatException |
                         CorruptedWorldException | WorldLockedException | IOException ex) {
                    String message;

                    if (ex instanceof IllegalArgumentException) {
                        message = ex.getMessage();

                        ex.printStackTrace();
                    } else if (ex instanceof UnknownWorldException) {
                        message = "world does not exist, are you sure you've set the correct data source?";
                    } else if (ex instanceof WorldLockedException) {
                        message = "world is in use! If you think this is a mistake, please wait some time and try again.";
                    } else if (ex instanceof NewerFormatException) {
                        message = "world is serialized in a newer Slime Format version (" + ex.getMessage() + ") that SWM does not understand.";
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
    public SlimeWorld loadWorld(SlimeLoader loader, String worldName, boolean readOnly, SlimePropertyMap propertyMap) throws UnknownWorldException, IOException,
            CorruptedWorldException, NewerFormatException, WorldLockedException {
        Objects.requireNonNull(loader, "Loader cannot be null");
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(propertyMap, "Properties cannot be null");

        long start = System.currentTimeMillis();

        Logging.info("Loading world " + worldName + ".");
        byte[] serializedWorld = loader.loadWorld(worldName);

        SlimeWorld slimeWorld = SlimeWorldReaderRegistry.readWorld(loader, worldName, serializedWorld, propertyMap, readOnly);
        Logging.info("Applying datafixers for " + worldName + ".");
        SlimeWorld dataFixed = SlimeNMSBridge.instance().applyDataFixers(slimeWorld);

        if (!readOnly) loader.saveWorld(worldName, SlimeSerializer.serialize(dataFixed)); // Write dataFixed world back to loader

        Logging.info("World " + worldName + " loaded in " + (System.currentTimeMillis() - start) + "ms.");

        registerWorld(dataFixed);
        return dataFixed;
    }

    @Override
    public SlimeWorld getWorld(String worldName) {
        return loadedWorlds.get(worldName);
    }

    public List<SlimeWorld> getLoadedWorlds() {
        return ImmutableList.copyOf(loadedWorlds.values());
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
        SlimeWorld blackhole = new SkeletonSlimeWorld(worldName, loader, readOnly, Map.of(), new CompoundTag("", new CompoundMap()), propertyMap, BRIDGE_INSTANCE.getCurrentVersion());

        loader.saveWorld(worldName, SlimeSerializer.serialize(blackhole));

        Logging.info("World " + worldName + " created in " + (System.currentTimeMillis() - start) + "ms.");

        registerWorld(blackhole);
        return blackhole;
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
    public SlimeWorld loadWorld(SlimeWorld slimeWorld, boolean callWorldLoadEvent) throws WorldLockedException, UnknownWorldException, IOException {
        Objects.requireNonNull(slimeWorld, "SlimeWorld cannot be null");

        if (!slimeWorld.isReadOnly() && slimeWorld.getLoader() != null) {
            slimeWorld.getLoader().acquireLock(slimeWorld.getName());
        }
        SlimeWorldInstance instance = BRIDGE_INSTANCE.loadInstance(slimeWorld);
        SlimeWorld mirror = instance.getSlimeWorldMirror();

        Bukkit.getPluginManager().callEvent(new LoadSlimeWorldEvent(mirror));
        if (callWorldLoadEvent) {
            Bukkit.getPluginManager().callEvent(new WorldLoadEvent(instance.getBukkitWorld()));
        }

        registerWorld(mirror);
        return mirror;
    }

    @Override
    public void migrateWorld(String worldName, SlimeLoader currentLoader, SlimeLoader newLoader) throws IOException,
            WorldAlreadyExistsException, UnknownWorldException {
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(currentLoader, "Current loader cannot be null");
        Objects.requireNonNull(newLoader, "New loader cannot be null");

        if (newLoader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        byte[] serializedWorld = currentLoader.loadWorld(worldName);
        newLoader.saveWorld(worldName, serializedWorld);
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
    public void importWorld(File worldDir, String worldName, SlimeLoader loader) throws WorldAlreadyExistsException, InvalidWorldException, WorldLoadedException, WorldTooBigException, IOException {
        this.importVanillaWorld(worldDir, worldName, loader);
    }

    @Override
    public SlimeWorld importVanillaWorld(File worldDir, String worldName, SlimeLoader loader) throws WorldAlreadyExistsException, InvalidWorldException, WorldLoadedException, WorldTooBigException, IOException {
        Objects.requireNonNull(worldDir, "World directory cannot be null");
        Objects.requireNonNull(worldName, "World name cannot be null");
        Objects.requireNonNull(loader, "Loader cannot be null");

        if (loader.worldExists(worldName)) {
            throw new WorldAlreadyExistsException(worldName);
        }

        World bukkitWorld = Bukkit.getWorld(worldDir.getName());

        if (bukkitWorld != null && BRIDGE_INSTANCE.getInstance(bukkitWorld) == null) {
            throw new WorldLoadedException(worldDir.getName());
        }

        SlimeWorld world = AnvilWorldReader.INSTANCE.readFromData(worldDir);

        byte[] serializedWorld;

        try {
            serializedWorld = SlimeSerializer.serialize(world);
        } catch (IndexOutOfBoundsException ex) {
            throw new WorldTooBigException(worldDir.getName());
        }

        loader.saveWorld(worldName, serializedWorld);
        return world;
    }

    public static boolean isPaperMC() {
        return isPaperMC;
    }

    public static SWMPlugin getInstance() {
        return SWMPlugin.getPlugin(SWMPlugin.class);
    }
}
