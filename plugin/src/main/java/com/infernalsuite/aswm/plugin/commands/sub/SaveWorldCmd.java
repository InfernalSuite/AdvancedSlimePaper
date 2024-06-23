package com.infernalsuite.aswm.plugin.commands.sub;


import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SaveWorldCmd extends SlimeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(SaveWorldCmd.class);

    public SaveWorldCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swp|aswm|swm save <world>")
    @CommandDescription("Saves a world.")
    @Permission("swm.saveworld")
    public void saveWorld(CommandSender sender, @Argument(value = "world") SlimeWorld slimeWorld) {
        try {
            asp.saveWorld(slimeWorld);
            sender.sendMessage(COMMAND_PREFIX.append(
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

