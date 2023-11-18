package com.grinderwolf.swm.plugin.commands.sub;


import com.grinderwolf.swm.plugin.log.Logging;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SaveWorldCmd implements Subcommand {

    @Override
    public String getUsage() {
        return "save <world>";
    }

    @Override
    public String getDescription() {
        return "Saves a world.";
    }

    @Override
    public String getPermission() {
        return "swm.saveworld";
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 0) {
            World world = Bukkit.getWorld(args[0]);

            if (world == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + args[0] + " is not loaded!");

                return true;
            }

            world.save();

            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GREEN + "World " + ChatColor.YELLOW + args[0] + ChatColor.GREEN + " saved correctly.");

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 0) {
            List<String> completes = new ArrayList<>();
            for (World world : Bukkit.getWorlds()) {
                completes.add(world.getName());
            }
            return completes;
        }

        return Collections.emptyList();
    }
}

