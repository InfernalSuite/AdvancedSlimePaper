package com.grinderwolf.swm.plugin.commands.sub;

import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.Utils.WorldManager;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldData;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import com.grinderwolf.swm.plugin.log.Logging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class WarpCmd implements Subcommand {

    boolean LoadOnWarp = ConfigManager.getDatasourcesConfig().getWorldConfig().isLoadOnWarp();

    @Override
    public String getUsage() {
        return "warp <world> [player]";
    }

    @Override
    public String getDescription() {
        return "Teleport yourself (or someone else) to a world.";
    }

    @Override
    public String getPermission() {
        return "swm.warp";
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 0) {
            World world = Bukkit.getWorld(args[0]);
            WorldsConfig config = ConfigManager.getWorldConfig();
            WorldData worldData = config.getWorlds().get(args[0]);


            if (world == null) {
                if (worldData == null) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + args[0] + " does not exist!");
                    return true;
                } else if (LoadOnWarp) {
                    if (WorldManager.loadWorld(args)) {
                        Bukkit.getScheduler().runTaskLaterAsynchronously(SWMPlugin.getInstance(), () -> gotoWorld(sender, args), 25);
                        return true;
                    }
                }
            }
            return gotoWorld(sender,args);
        }
        return false;
    }

    private boolean gotoWorld(CommandSender sender, String[] args) {

        World world = Bukkit.getWorld(args[0]);
        Player target;
        if (args.length > 1) {
            target = Bukkit.getPlayerExact(args[1]);
        } else {
            if (!(sender instanceof Player)) {
                if (args.length > 2) {
                    target = Bukkit.getPlayerExact(args[2]);
                }

                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "The console cannot be teleported to a world! Please specify a player.");

                return true;
            }

            target = (Player) sender;
        }

        if (target == null) {
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + args[1] + " is offline.");

            return true;
        }

        sender.sendMessage(Logging.COMMAND_PREFIX + "Teleporting " + (target.getName().equals(sender.getName())
                ? "yourself" : ChatColor.YELLOW + target.getName() + ChatColor.GRAY) + " to " + ChatColor.AQUA + world.getName() + ChatColor.GRAY + "...");

        Location spawnLocation;
        if (ConfigManager.getWorldConfig().getWorlds().containsKey(world.getName())) {
            String spawn = ConfigManager.getWorldConfig().getWorlds().get(world.getName()).getSpawn();
            String[] coords = spawn.split(", ");
            double x = Double.parseDouble(coords[0]);
            double y = Double.parseDouble(coords[1]);
            double z = Double.parseDouble(coords[2]);
            spawnLocation = new Location(world, x, y, z);
        } else {
            spawnLocation = world.getSpawnLocation();
        }

        World CurrentWorld = target.getWorld();
        List<Player> players = world.getPlayers();
        boolean HasMoved;
        if (SWMPlugin.isPaperMC()) {
            target.teleportAsync(spawnLocation);
            HasMoved = true;
        } else {
            target.teleport(spawnLocation);
            HasMoved = true;
        }
        if (LoadOnWarp && players.isEmpty() && HasMoved) {WorldManager.unloadWorld(CurrentWorld);}

        return true;
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> toReturn = null;

        if (sender instanceof ConsoleCommandSender) {
            return Collections.emptyList();
        }

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
            final String typed = args[2].toLowerCase();

            for (Player player : Bukkit.getOnlinePlayers()) {
                final String playerName = player.getName();
                if (playerName.toLowerCase().startsWith(typed)) {
                    if (toReturn == null) {
                        toReturn = new LinkedList<>();
                    }
                    toReturn.add(playerName);
                }
            }
        }

        return toReturn == null ? Collections.emptyList() : toReturn;
    }
}
