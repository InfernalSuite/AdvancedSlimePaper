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
import java.util.List;

public class LoadTemplateWorldCmd implements Subcommand {

    @Override
    public String getUsage() {
        return "load-template <template-world> <world-name>";
    }

    @Override
    public String getDescription() {
        return "Creates a temporary world using another as a template. This world will never be stored.";
    }

    @Override
    public String getPermission() {
        return "swm.loadworld.template";
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 1) {
            String worldName = args[1];
            World world = Bukkit.getWorld(worldName);

            if (world != null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already loaded!");

                return true;
            }

            String templateWorldName = args[0];

            WorldsConfig config = ConfigManager.getWorldConfig();
            WorldData worldData = config.getWorlds().get(templateWorldName);

            if (worldData == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to find world " + templateWorldName + " inside the worlds config file.");

                return true;
            }

            if (templateWorldName.equals(worldName)) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "The template world name cannot be the same as the cloned world one!");

                return true;
            }

            if (CommandManager.getInstance().getWorldsInUse().contains(worldName)) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already being used on another command! Wait some time and try again.");

                return true;
            }

            CommandManager.getInstance().getWorldsInUse().add(worldName);
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GRAY + "Creating world " + ChatColor.YELLOW + worldName
                    + ChatColor.GRAY + " using " + ChatColor.YELLOW + templateWorldName + ChatColor.GRAY + " as a template...");

            // It's best to load the world async, and then just go back to the server thread and add it to the world list
            Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {

                try {
                    long start = System.currentTimeMillis();
                    SlimeLoader loader = SWMPlugin.getInstance().getLoader(worldData.getDataSource());

                    if (loader == null) {
                        throw new IllegalArgumentException("invalid data source " + worldData.getDataSource());
                    }

                    SlimeWorld slimeWorld = SWMPlugin.getInstance().loadWorld(loader, templateWorldName, true, worldData.toPropertyMap()).clone(worldName);
                    Bukkit.getScheduler().runTask(SWMPlugin.getInstance(), () -> {
                        try {
                            SWMPlugin.getInstance().loadWorld(slimeWorld, true);
                        } catch (IllegalArgumentException ex) {
                            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to generate world " + worldName + ": " + ex.getMessage() + ".");

                            return;
                        } catch(WorldLockedException | UnknownWorldException | IOException exception) {
                            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world when making template " + worldName + ": " + exception.getMessage() + ".");
                            SWMPlugin.getInstance().getLogger().info("Failed to load world when making template " + worldName + ": " + exception.getMessage());
                            return;
                        }

                        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName
                                + ChatColor.GREEN + " loaded and generated in " + (System.currentTimeMillis() - start) + "ms!");
                    });
                } catch (CorruptedWorldException ex) {
                    if (!(sender instanceof ConsoleCommandSender)) {
                        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + templateWorldName +
                                ": world seems to be corrupted.");
                    }

                    Logging.error("Failed to load world " + templateWorldName + ": world seems to be corrupted.");
                    ex.printStackTrace();
                } catch (NewerFormatException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + templateWorldName + ": this world" +
                            " was serialized with a newer version of the Slime Format (" + ex.getMessage() + ") that SWM cannot understand.");
                } catch (UnknownWorldException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + templateWorldName +
                            ": world could not be found (using data source '" + worldData.getDataSource() + "').");
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + templateWorldName +
                            ": " + ex.getMessage());
                } catch (WorldLockedException ignored) {

                } catch (IOException ex) {
                    if (!(sender instanceof ConsoleCommandSender)) {
                        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world " + templateWorldName
                                + ". Take a look at the server console for more information.");
                    }

                    Logging.error("Failed to load world " + templateWorldName + ":");
                    ex.printStackTrace();
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
        return Collections.emptyList();
    }
}

