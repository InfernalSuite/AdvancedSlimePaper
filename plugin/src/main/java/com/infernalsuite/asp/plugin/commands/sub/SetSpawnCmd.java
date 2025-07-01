package com.infernalsuite.asp.plugin.commands.sub;


import com.infernalsuite.asp.plugin.commands.CommandManager;
import com.infernalsuite.asp.plugin.commands.SlimeCommand;
import com.infernalsuite.asp.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.asp.plugin.config.ConfigManager;
import com.infernalsuite.asp.plugin.config.WorldData;
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
import org.incendo.cloud.paper.util.sender.PlayerSource;
import org.incendo.cloud.paper.util.sender.Source;

public class SetSpawnCmd extends SlimeCommand {

    public SetSpawnCmd(CommandManager commandManager) {
        super(commandManager);
    }

    //TODO: It seems like originally this command was supposed to allow to set a spawnpoint based on a provided location, but it was never implemented.
    @Command("swp|aswm|swm setspawn")
    @CommandDescription("Set the spawnpoint of a world based on your location")
    @Permission("swm.setspawn")
    public void setSpawn(Source sender) {
        if (!(sender instanceof PlayerSource playerSource)) {
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("This command is for players").color(NamedTextColor.RED)
            ));
        }
        Player player = playerSource.source();

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

        player.sendMessage(COMMAND_PREFIX.append(
                Component.text("Set spawn for ").color(NamedTextColor.GREEN)
                        .append(Component.text(world.getName()).color(NamedTextColor.YELLOW))
                        .append(Component.text(".").color(NamedTextColor.GREEN))
        ));
    }
}

