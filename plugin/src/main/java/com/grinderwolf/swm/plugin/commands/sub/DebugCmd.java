package com.grinderwolf.swm.plugin.commands.sub;


import com.grinderwolf.swm.nms.SlimeLogger;
import com.grinderwolf.swm.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

@Getter
public class DebugCmd implements Subcommand {

    private final String usage = "debug";
    private final String description = "Toggles debug messages";
    private final String permission = "swm.debug";

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

