package com.grinderwolf.swm.plugin.commands.sub;


import com.grinderwolf.swm.plugin.Utils.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

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
        return WorldManager.loadWorld(args);
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

