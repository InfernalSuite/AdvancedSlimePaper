package com.grinderwolf.swm.plugin.commands.sub;


import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import com.grinderwolf.swm.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
public class SetSpawnCmd implements Subcommand {

    private final String usage = "setspawn (world) (x) (y) (z) (yaw) (pitch)";
    private final String description = "Set the spawnpoint of a world based on your location or one provided.";
    private final String permission = "swm.setspawn";

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if(!(sender instanceof Player)) {
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "This command is for players");
        }

        if (args.length > 0) {
            World world = Bukkit.getWorld(args[0]);

            if (world == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + args[0] + " does not exist!");

                return true;
            }

            Player player = (Player) sender;

            world.setSpawnLocation(player.getLocation());

            WorldsConfig config = ConfigManager.getWorldConfig();

            if (!(config.getWorlds().containsKey(world.getName()))) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + ChatColor.YELLOW + world.getName() + ChatColor.RED + " is not a slimeworld.");

                return true;
            }

            String spawnVerbose = player.getLocation().getX() + ", " + player.getLocation().getY() + ", " + player.getLocation().getZ();

            config.getWorlds().get(world.getName()).setSpawn(spawnVerbose);
            config.save();

            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GREEN + "Set spawn for " + ChatColor.YELLOW + args[0] + ChatColor.GREEN + ".");

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> completes = new ArrayList<>();
        if(sender instanceof Player) {
            if (args.length == 0) {
                completes.clear();
                for (World world : Bukkit.getWorlds()) {
                    completes.add(world.getName());
                }
                return completes;
            } else if (args.length == 1) {
                completes.clear();
                for (World world : Bukkit.getWorlds()) {
                    completes.add(world.getName());
                }
                return completes;
            }

            return Collections.emptyList();
        }
        return completes;
    }
}

