package com.grinderwolf.swm.plugin.commands.sub;

import com.grinderwolf.swm.plugin.Utils.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class UnloadWorldCmd implements Subcommand {

    @Override
    public String getUsage() {
        return "unload <world> [data-source]";
    }

    @Override
    public String getDescription() {
        return "Unload a world.";
    }

    @Override
    public String getPermission() {
        return "swm.unloadworld";
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        return WorldManager.unloadWorld(args);
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

