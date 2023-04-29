package com.grinderwolf.swm.plugin.commands.sub;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldData;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.log.Logging;
import com.infernalsuite.aswm.api.exceptions.InvalidWorldException;
import com.infernalsuite.aswm.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.aswm.api.exceptions.WorldLoadedException;
import com.infernalsuite.aswm.api.exceptions.WorldTooBigException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ImportWorldCmd implements Subcommand {

    @Override
    public String getUsage() {
        return "import <path-to-world> <data-source> [new-world-name]";
    }

    @Override
    public String getDescription() {
        return "Convert a world to the slime format and save it.";
    }

    @Override
    public String getPermission() {
        return "swm.importworld";
    }

    private final Cache<String, String[]> importCache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.MINUTES).build();

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 1) {
            String dataSource = args[1];
            SlimeLoader loader = LoaderUtils.getLoader(dataSource);

            if (loader == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Data source " + dataSource + " does not exist.");

                return true;
            }

            File worldDir = new File(args[0]);

            if (!worldDir.exists() || !worldDir.isDirectory()) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Path " + worldDir.getPath() + " does not point out to a valid world directory.");

                return true;
            }

            String[] oldArgs = importCache.getIfPresent(sender.getName());

            if (oldArgs != null) {
                importCache.invalidate(sender.getName());

                if (Arrays.equals(args, oldArgs)) { // Make sure it's exactly the same command
                    String worldName = (args.length > 2 ? args[2] : worldDir.getName());
                    sender.sendMessage(Logging.COMMAND_PREFIX + "Importing world " + worldDir.getName() + " into data source " + dataSource + "...");

                    WorldsConfig config = ConfigManager.getWorldConfig();

                    if (config.getWorlds().containsKey(worldName)) {
                        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "There is already a world called  " + worldName + " inside the worlds config file.");

                        return true;
                    }

                    Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {

                        try {
                            long start = System.currentTimeMillis();
                            SlimeWorld world = SWMPlugin.getInstance().importVanillaWorld(worldDir, worldName, loader);

                            sender.sendMessage(Logging.COMMAND_PREFIX +  ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName + ChatColor.GREEN + " imported " +
                                    "successfully in " + (System.currentTimeMillis() - start) + "ms.");

                            WorldData worldData = new WorldData();
                            StringBuilder spawn = new StringBuilder();
                            for(String key : world.getPropertyMap().getProperties().keySet()) {
                                switch (key.toLowerCase()) {
                                    case "spawnx" -> spawn.append(world.getPropertyMap().getValue(SlimeProperties.SPAWN_X)).append(", ");
                                    case "spawny" -> spawn.append(world.getPropertyMap().getValue(SlimeProperties.SPAWN_Y)).append(", ");
                                    case "spawnz" -> spawn.append(world.getPropertyMap().getValue(SlimeProperties.SPAWN_Z));
                                    case "environment" -> worldData.setEnvironment(world.getPropertyMap().getValue(SlimeProperties.ENVIRONMENT));
                                    case "difficulty" -> worldData.setDifficulty(world.getPropertyMap().getValue(SlimeProperties.DIFFICULTY).toLowerCase());
                                    case "allowmonsters" -> worldData.setAllowMonsters(world.getPropertyMap().getValue(SlimeProperties.ALLOW_MONSTERS));
                                    case "dragonbattle" -> worldData.setDragonBattle(world.getPropertyMap().getValue(SlimeProperties.DRAGON_BATTLE));
                                    case "pvp" -> worldData.setPvp(world.getPropertyMap().getValue(SlimeProperties.PVP));
                                    case "worldtype" -> worldData.setWorldType(world.getPropertyMap().getValue(SlimeProperties.WORLD_TYPE));
                                    case "defaultbiome" -> worldData.setDefaultBiome(world.getPropertyMap().getValue(SlimeProperties.DEFAULT_BIOME));
                                }
                            }

                            worldData.setDataSource(dataSource);
                            worldData.setSpawn(spawn.toString().isEmpty() ? "0.5, 255, 0.5" : spawn.toString());
                            config.getWorlds().put(worldName, worldData);
                            config.save();

                        } catch (WorldAlreadyExistsException ex) {
                            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Data source " + dataSource + " already contains a world called " + worldName + ".");
                        } catch (InvalidWorldException ex) {
                            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Directory " + worldDir.getName() + " does not contain a valid Minecraft world.");
                        } catch (WorldLoadedException ex) {
                            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldDir.getName() + " is loaded on this server. Please unload it before importing it.");
                        } catch (WorldTooBigException ex) {
                            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Hey! Didn't you just read the warning? The Slime Format isn't meant for big worlds." +
                                    " The world you provided just breaks everything. Please, trim it by using the MCEdit tool and try again.");
                        } catch (IOException ex) {
                            if (!(sender instanceof ConsoleCommandSender)) {
                                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to import world " + worldName
                                        + ". Take a look at the server console for more information.");
                            }

                            Logging.error("Failed to import world " + worldName + ". Stack trace:");
                            ex.printStackTrace();
                        }

                    });

                    return true;
                }
            }

            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + ChatColor.BOLD + "WARNING: " + ChatColor.GRAY + "The Slime Format is meant to " +
                    "be used on tiny maps, not big survival worlds. It is recommended to trim your world by using the Prune MCEdit tool to ensure " +
                    "you don't save more chunks than you want to.");

            sender.sendMessage(" ");
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.YELLOW + ChatColor.BOLD + "NOTE: " + ChatColor.GRAY + "This command will automatically ignore every " +
                    "chunk that doesn't contain any blocks.");
            sender.sendMessage(" ");
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GRAY + "If you are sure you want to continue, type again this command.");

            importCache.put(sender.getName(), args);

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> toReturn = null;

        if (args.length == 3) {
            return new LinkedList<>(LoaderUtils.getAvailableLoadersNames());
        }

        return Collections.emptyList();
    }
}

