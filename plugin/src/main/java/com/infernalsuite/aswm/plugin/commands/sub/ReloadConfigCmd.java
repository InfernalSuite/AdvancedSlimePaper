package com.infernalsuite.aswm.plugin.commands.sub;

import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ReloadConfigCmd extends SlimeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReloadConfigCmd.class);

    public ReloadConfigCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swp|aswm|swm reload")
    @CommandDescription("Reloads the config files.")
    @Permission("swm.reload")
    public CompletableFuture<Void> reloadConfig(CommandSender sender) {
        return CompletableFuture.runAsync(() -> {
            try {
                ConfigManager.initialize();
            } catch (IOException ex) {
                LOGGER.error("Failed to load config files:", ex);

                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to reload the config file. Take a look at the server console for more information.").color(NamedTextColor.RED)
                ));
            }

            sender.sendMessage(COMMAND_PREFIX.append(
                    Component.text("Config reloaded.").color(NamedTextColor.GREEN)
            ));
        });
    }
}

