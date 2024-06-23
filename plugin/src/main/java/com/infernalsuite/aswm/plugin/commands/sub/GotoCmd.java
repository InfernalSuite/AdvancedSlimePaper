package com.infernalsuite.aswm.plugin.commands.sub;

import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.jetbrains.annotations.Nullable;

public class GotoCmd extends SlimeCommand {

    public GotoCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swp|aswm|swm goto <world> [player]")
    @CommandDescription("Teleport yourself (or someone else) to a world.")
    @Permission("swm.goto")
    public void onCommand(CommandSender sender, @Argument(value = "world") World world,
                             @Argument(value = "player") @Nullable Player target ) {
        Player finalTarget;

        if (target == null) {
            if (!(sender instanceof Player)) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("The console cannot be teleported to a world! Please specify a player.").color(NamedTextColor.RED)
                ));
            }

            finalTarget = (Player) sender;
        } else {
            finalTarget = target;
        }

        if (target == null) {
            sender.sendMessage(COMMAND_PREFIX.append(
                    Component.text("Teleporting yourself to ").color(NamedTextColor.GRAY)
                            .append(Component.text(world.getName()).color(NamedTextColor.AQUA))
                            .append(Component.text("...")).color(NamedTextColor.GRAY)
            ));
        } else {
            sender.sendMessage(COMMAND_PREFIX.append(
                    Component.text("Teleporting ").color(NamedTextColor.GRAY)
                            .append(Component.text(target.getName()).color(NamedTextColor.YELLOW))
                            .append(Component.text(" to ").color(NamedTextColor.GRAY))
                            .append(Component.text(world.getName()).color(NamedTextColor.AQUA))
                            .append(Component.text("...")).color(NamedTextColor.GRAY)
            ));
        }

        Location spawnLocation;
        if (ConfigManager.getWorldConfig().getWorlds().containsKey(world.getName())) {
            String spawn = ConfigManager.getWorldConfig().getWorlds().get(world.getName()).getSpawn();
            String[] coords = spawn.split(", ");
            double x = Double.parseDouble(coords[0]);
            double y = Double.parseDouble(coords[1]);
            double z = Double.parseDouble(coords[2]);
            spawnLocation = new Location(world, x, y, z);
        } else {
            spawnLocation = world.getSpawnLocation();
        }

        finalTarget.teleportAsync(spawnLocation);
    }
}
