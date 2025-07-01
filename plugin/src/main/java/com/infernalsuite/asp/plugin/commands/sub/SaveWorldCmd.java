package com.infernalsuite.asp.plugin.commands.sub;


import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.plugin.commands.CommandManager;
import com.infernalsuite.asp.plugin.commands.SlimeCommand;
import com.infernalsuite.asp.plugin.commands.exception.MessageCommandException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SaveWorldCmd extends SlimeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveWorldCmd.class);

    public SaveWorldCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swp|aswm|swm save <world>")
    @CommandDescription("Saves a world.")
    @Permission("swm.saveworld")
    public void saveWorld(Source sender, @Argument(value = "world") SlimeWorld slimeWorld) {
        try {
            asp.saveWorld(slimeWorld);
            sender.source().sendMessage(COMMAND_PREFIX.append(
                    Component.text("World " + slimeWorld.getName() + " saved.").color(NamedTextColor.GREEN)
            ));
        } catch (IOException e) {
            LOGGER.error("Failed to save world {}.", slimeWorld.getName(), e);
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("Failed to save world " + slimeWorld.getName() + ".").color(NamedTextColor.RED)
            ));
        }

    }
}

