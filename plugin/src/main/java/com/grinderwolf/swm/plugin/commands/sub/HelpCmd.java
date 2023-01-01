package com.grinderwolf.swm.plugin.commands.sub;

import com.grinderwolf.swm.plugin.commands.CommandManager;
import com.grinderwolf.swm.plugin.log.Logging;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class HelpCmd implements Subcommand {

    @Override
    public String getUsage() {
        return "help";
    }

    @Override
    public String getDescription() {
        return "Shows this page.";
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        sender.sendMessage(Logging.COMMAND_PREFIX + "Command list:");

        for (Subcommand cmd : CommandManager.getInstance().getCommands()) {
            if (cmd.inGameOnly() && !(sender instanceof Player) || (!cmd.getPermission().equals("") && !sender.hasPermission(cmd.getPermission()) && !sender.hasPermission("swm.*"))) {
                continue;
            }

            sender.sendMessage(ChatColor.GRAY + "  -" + ChatColor.AQUA + "/swm " + cmd.getUsage() + ChatColor.GRAY + " - " + cmd.getDescription());
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
