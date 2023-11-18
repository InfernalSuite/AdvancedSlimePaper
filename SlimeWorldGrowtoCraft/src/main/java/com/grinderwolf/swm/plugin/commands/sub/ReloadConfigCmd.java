package com.grinderwolf.swm.plugin.commands.sub;

import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.log.Logging;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

public class ReloadConfigCmd implements Subcommand {

    @Override
    public String getUsage() {
        return "reload";
    }

    @Override
    public String getDescription() {
        return "Reloads the config files.";
    }

    @Override
    public String getPermission() {
        return "swm.reload";
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        try {
            ConfigManager.initialize();
        } catch (IOException ex) {
            if (!(sender instanceof ConsoleCommandSender)) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to reload the config file. Take a look at the server console for more information.");
            }

            Logging.error("Failed to load config files:");
            ex.printStackTrace();

            return true;
        }

        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GREEN + "Config reloaded.");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}

