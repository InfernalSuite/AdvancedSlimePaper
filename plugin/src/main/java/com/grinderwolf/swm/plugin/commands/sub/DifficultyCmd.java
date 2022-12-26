package com.grinderwolf.swm.plugin.commands.sub;

import org.bukkit.command.CommandSender;

import java.util.List;

public class DifficultyCmd implements Subcommand {

    private final String usage = "difficulty <difficulty> (<world>)";
    private final String description = "Changes world difficulty";
    private final String permission = "swm.difficulty";

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }

    @Override
    public String getUsage() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }
}
