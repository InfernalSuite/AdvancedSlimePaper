package com.grinderwolf.swm.plugin.commands.sub;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface Subcommand {

    boolean onCommand(CommandSender sender, String[] args);

    List<String> onTabComplete(CommandSender sender, String[] args);

    String getUsage();

    String getDescription();

    default boolean inGameOnly() {
        return false;
    }

    default String getPermission() {
        return "";
    }
}
