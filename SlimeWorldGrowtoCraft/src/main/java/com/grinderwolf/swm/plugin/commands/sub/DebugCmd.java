package com.grinderwolf.swm.plugin.commands.sub;


import com.grinderwolf.swm.plugin.log.Logging;
import com.infernalsuite.aswm.SlimeLogger;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

public class DebugCmd implements Subcommand {

    @Override
    public String getUsage() {
        return "debug";
    }

    @Override
    public String getDescription() {
        return "Toggles debug messages";
    }

    @Override
    public String getPermission() {
        return "swm.debug";
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        SlimeLogger.DEBUG = !SlimeLogger.DEBUG;

        sender.sendMessage(Logging.COMMAND_PREFIX + "Debug messages: " + SlimeLogger.DEBUG);

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

