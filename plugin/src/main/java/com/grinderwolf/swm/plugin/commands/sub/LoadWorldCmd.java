package com.grinderwolf.swm.plugin.commands.sub;


import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.commands.CommandManager;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldData;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import com.grinderwolf.swm.plugin.log.Logging;
import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.exceptions.WorldLockedException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class LoadWorldCmd implements Subcommand {

    @Override
    public String getUsage() {
        return "load <world>";
    }

    @Override
    public String getDescription() {
        return "Load a world.";
    }

    @Override
    public String getPermission() {
        return "swm.loadworld";
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 0) {
            String worldName = args[0];
            World world = Bukkit.getWorld(worldName);

            if (world != null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already loaded!");

                return true;
            }

            WorldsConfig config = ConfigManager.getWorldConfig();
            WorldData worldData = config.getWorlds().get(worldName);

            if (worldData == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to find world " + worldName + " inside the worlds config file.");

                return true;
            }

            if (CommandManager.getInstance().getWorldsInUse().contains(worldName)) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already being used on another command! Wait some time and try again.");

                return true;
            }

            CommandManager.getInstance().getWorldsInUse().add(worldName);
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GRAY + "Loading world " + ChatColor.YELLOW + worldName + ChatColor.GRAY + "...");

            // It's best to load the world async, and then just go back to the server thread and add it to the world list
            Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {

                try {
                    // ATTEMPT TO LOAD WORLD
                    long start = System.currentTimeMillis();
                    SlimeLoader loader = SWMPlugin.getInstance().getLoader(worldData.getDataSource());

                    if (loader == null) {
                        throw new IllegalArgumentException("invalid data source " + worldData.getDataSource());
                    }

                    SlimeWorld slimeWorld = SWMPlugin.getInstance().loadWorld(loader, worldName, worldData.isReadOnly(), worldData.toPropertyMap());
                    Bukkit.getScheduler().runTask(SWMPlugin.getInstance(), () -> {
                        try {
                            SWMPlugin.getInstance().loadWorld(slimeWorld, true);
                        } catch (IllegalArgumentException ex) {
                            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to generate world " + worldName + ": " + ex.getMessage() + ".");

                            return;
                        } catch(WorldLockedException | UnknownWorldException | IOException exception) {
                            SWMPlugin.getInstance().getLogger().info("Failed to load world " + worldName + ": " + exception.getMessage());
                            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName + ": " + exception.getMessage() + ".");
                            return;
                        }

                        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName
                                + ChatColor.GREEN + " loaded and generated in " + (System.currentTimeMillis() - start) + "ms!");
                    });
                } catch (CorruptedWorldException ex) {
                    if (!(sender instanceof ConsoleCommandSender)) {
                        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName +
                                ": world seems to be corrupted.");
                    }

                    Logging.error("Failed to load world " + worldName + ": world seems to be corrupted.");
                    ex.printStackTrace();
                } catch (NewerFormatException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName + ": this world" +
                            " was serialized with a newer version of the Slime Format (" + ex.getMessage() + ") that SWM cannot understand.");
                } catch (UnknownWorldException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName +
                            ": world could not be found (using data source '" + worldData.getDataSource() + "').");
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName +
                            ": " + ex.getMessage());
                } catch (IOException ex) {
                    if (!(sender instanceof ConsoleCommandSender)) {
                        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName
                                + ". Take a look at the server console for more information.");
                    }

                    Logging.error("Failed to load world " + worldName + ":");
                    ex.printStackTrace();
                } catch (WorldLockedException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + worldName +
                            ": world is already in use. If you think this is a mistake, please wait some time and try again.");
                } finally {
                    CommandManager.getInstance().getWorldsInUse().remove(worldName);
                }
            });

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> toReturn = null;

        if (args.length == 2) {
            final String typed = args[1].toLowerCase();

            for (World world : Bukkit.getWorlds()) {
                final String worldName = world.getName();
                if (worldName.toLowerCase().startsWith(typed)) {
                    if (toReturn == null) {
                        toReturn = new LinkedList<>();
                    }

                    toReturn.add(worldName);
                }
            }
        }

        return toReturn == null ? Collections.emptyList() : toReturn;
    }
}

