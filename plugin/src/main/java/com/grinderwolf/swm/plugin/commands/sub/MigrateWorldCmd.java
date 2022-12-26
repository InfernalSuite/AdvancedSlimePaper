package com.grinderwolf.swm.plugin.commands.sub;

import com.grinderwolf.swm.api.exceptions.UnknownWorldException;
import com.grinderwolf.swm.api.exceptions.WorldAlreadyExistsException;
import com.grinderwolf.swm.api.exceptions.WorldInUseException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.commands.CommandManager;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldData;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Getter
public class MigrateWorldCmd implements Subcommand {

    private final String usage = "migrate <world> <new-data-source>";
    private final String description = "Migrate a world from one data source to another.";
    private final String permission = "swm.migrate";

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 1) {
            String worldName = args[0];
            WorldsConfig config = ConfigManager.getWorldConfig();
            WorldData worldData = config.getWorlds().get(worldName);

            if (worldData == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown world " + worldName + "! Are you sure you configured it correctly?");

                return true;
            }

            String newSource = args[1];
            SlimeLoader newLoader = LoaderUtils.getLoader(newSource);

            if (newLoader == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown data source " + newSource + "!");

                return true;
            }

            String currentSource = worldData.getDataSource();

            if (newSource.equalsIgnoreCase(currentSource)) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already stored using data source " + currentSource + "!");

                return true;
            }

            SlimeLoader oldLoader = LoaderUtils.getLoader(currentSource);

            if (oldLoader == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown data source " + currentSource + "! Are you sure you configured it correctly?");

                return true;
            }

            if (CommandManager.getInstance().getWorldsInUse().contains(worldName)) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is already being used on another command! Wait some time and try again.");

                return true;
            }

            CommandManager.getInstance().getWorldsInUse().add(worldName);

            Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {

                try {
                    long start = System.currentTimeMillis();
                    SWMPlugin.getInstance().migrateWorld(worldName, oldLoader, newLoader);

                    worldData.setDataSource(newSource);
                    config.save();

                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName + ChatColor.GREEN + " migrated in "
                            + (System.currentTimeMillis() - start) + "ms!");
                } catch (IOException ex) {
                    if (!(sender instanceof ConsoleCommandSender)) {
                        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to migrate world " + worldName + " (using data sources "
                                + currentSource + " and " + newSource + "). Take a look at the server console for more information.");
                    }

                    Logging.error("Failed to load world " + worldName + " (using data source " + currentSource + "):");
                    ex.printStackTrace();
                } catch (WorldInUseException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is being used on another server.");
                } catch (WorldAlreadyExistsException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Data source " + newSource + " already contains a world named " + worldName + "!");
                } catch (UnknownWorldException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Can't find world " + worldName + " in data source " + currentSource + ".");
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

        if (args.length == 3) {
            toReturn = new LinkedList<>(LoaderUtils.getAvailableLoadersNames());
        }

        return toReturn == null ? Collections.emptyList() : toReturn;
    }
}

