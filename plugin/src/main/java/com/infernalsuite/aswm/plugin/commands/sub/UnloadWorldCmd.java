package com.infernalsuite.aswm.plugin.commands.sub;

import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.plugin.SWPlugin;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class UnloadWorldCmd extends SlimeCommand {

    public UnloadWorldCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swp|aswm|swm unload <world>")
    @CommandDescription("Unload a world.")
    @Permission("swm.unloadworld")
    public void unloadWorld(CommandSender sender, @Argument(value = "world") SlimeWorld slimeWorld) {
        var bukkitWorld = Bukkit.getWorld(slimeWorld.getName());

        // Teleport all players outside the world before unloading it
        var players = bukkitWorld.getPlayers();

        if (!players.isEmpty()) {
            Location spawnLocation = findValidDefaultSpawn();
            CompletableFuture<Void> cf = CompletableFuture.allOf(players.stream().map(player -> player.teleportAsync(spawnLocation)).toList().toArray(CompletableFuture[]::new));
            cf.thenRun(() -> {
                Bukkit.getScheduler().runTask(SWPlugin.getInstance(), () -> {
                    boolean success = Bukkit.unloadWorld(bukkitWorld, true);

                    if (!success) {
                        sender.sendMessage(COMMAND_PREFIX.append(
                                Component.text("Failed to unload world " + slimeWorld.getName() + ".").color(NamedTextColor.RED)
                        ));
                    } else {
                        sender.sendMessage(COMMAND_PREFIX.append(
                                Component.text("World ").color(NamedTextColor.GREEN)
                                        .append(Component.text(slimeWorld.getName()).color(NamedTextColor.YELLOW))
                                        .append(Component.text(" unloaded correctly.")).color(NamedTextColor.GREEN)
                        ));
                    }
                });
            });
        } else {
            Bukkit.unloadWorld(bukkitWorld, true);
            sender.sendMessage(COMMAND_PREFIX.append(
                    Component.text("World ").color(NamedTextColor.GREEN)
                            .append(Component.text(slimeWorld.getName()).color(NamedTextColor.YELLOW))
                            .append(Component.text(" unloaded correctly.")).color(NamedTextColor.GREEN)
            ));
        }
    }

    @NotNull
    private Location findValidDefaultSpawn() {
        var defaultWorld = Bukkit.getWorlds().getFirst();
        var spawnLocation = defaultWorld.getSpawnLocation();

        spawnLocation.setY(64);
        while (spawnLocation.getBlock().getType() != Material.AIR || spawnLocation.getBlock().getRelative(BlockFace.UP).getType() != Material.AIR) {
            if (spawnLocation.getY() >= 320) {
                spawnLocation.add(0, 1, 0);
                break;
            }

            spawnLocation.add(0, 1, 0);
        }
        return spawnLocation;
    }
}

