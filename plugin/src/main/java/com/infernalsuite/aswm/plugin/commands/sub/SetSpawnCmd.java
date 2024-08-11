package com.infernalsuite.aswm.plugin.commands.sub;


import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.config.ConfigManager;
import com.infernalsuite.aswm.plugin.config.WorldData;
import com.infernalsuite.aswm.plugin.config.WorldsConfig;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SetSpawnCmd extends SlimeCommand {

    public SetSpawnCmd(CommandManager commandManager) {
        super(commandManager);
    }

    //TODO: It seems like originally this command was supposed to allow to set a spawnpoint based on a provided location, but it was never implemented.
    @Command("swp|aswm|swm setspawn")
    @CommandDescription("Set the spawnpoint of a world based on your location")
    @Permission("swm.setspawn")
    public void setSpawn(CommandSender sender) {
        if (!(sender instanceof Player player)) {
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("This command is for players").color(NamedTextColor.RED)
            ));
        }

        Location location = player.getLocation();
        World world = location.getWorld();
        WorldData config = ConfigManager.getWorldConfig().getWorlds().get(world.getName());

        if (config == null) {
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World ").color(NamedTextColor.RED)
                            .append(Component.text(world.getName()).color(NamedTextColor.YELLOW))
                            .append(Component.text(" is not a registered slime world.")).color(NamedTextColor.RED)
            ));
        }

        world.setSpawnLocation(player.getLocation());

        String spawnVerbose = player.getLocation().getX() + ", " + player.getLocation().getY() + ", " + player.getLocation().getZ();

        config.setSpawn(spawnVerbose);
        ConfigManager.getWorldConfig().save(); //FIXME: An IO op should be done async

        sender.sendMessage(COMMAND_PREFIX.append(
                Component.text("Set spawn for ").color(NamedTextColor.GREEN)
                        .append(Component.text(world.getName()).color(NamedTextColor.YELLOW))
                        .append(Component.text(".").color(NamedTextColor.GREEN))
        ));
    }
}

