package com.grinderwolf.swm.plugin.commands.sub;

import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldData;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.log.Logging;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class UnloadWorldCmd implements Subcommand {

    @Override
    public String getUsage() {
        return "unload <world> [data-source]";
    }

    @Override
    public String getDescription() {
        return "Unload a world.";
    }

    @Override
    public String getPermission() {
        return "swm.unloadworld";
    }

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            return false;
        }

        var worldName = args[0];
        var world = Bukkit.getWorld(args[0]);

        if (world == null) {
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "World " + worldName + " is not loaded!");

            return true;
        }

        String source = null;

        if (args.length > 1) {
            source = args[1];
        } else {
            WorldsConfig config = ConfigManager.getWorldConfig();
            WorldData worldData = config.getWorlds().get(worldName);

            if (worldData != null && !worldData.isReadOnly()) {
                source = worldData.getDataSource();
            }
        }

        var loader = source == null ? null : LoaderUtils.getLoader(source);

        // Teleport all players outside the world before unloading it
        var players = world.getPlayers();

        AtomicBoolean success = new AtomicBoolean();

        if (!players.isEmpty()) {
            Location spawnLocation = findValidDefaultSpawn();
            CompletableFuture<Void> cf = CompletableFuture.allOf(players.stream().map(player -> player.teleportAsync(spawnLocation)).collect(Collectors.toList()).toArray(CompletableFuture[]::new));
            cf.thenRun(() -> {
                Bukkit.getScheduler().runTask(SWMPlugin.getInstance(), () -> success.set(Bukkit.unloadWorld(world, true)));
                if (!success.get()) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to unload world " + worldName + ".");
                } else {
                    world.save();
                }
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName + ChatColor.GREEN + " unloaded correctly.");
            });
        } else {
            Bukkit.unloadWorld(world, true);
            sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GREEN + "World " + ChatColor.YELLOW + worldName + ChatColor.GREEN + " unloaded correctly.");
        }

        return true;
    }

    @NotNull
    private Location findValidDefaultSpawn() {
        var defaultWorld = Bukkit.getWorlds().get(0);
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

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> toReturn = null;

        if (args.length == 2) {
            final String typed = args[1].toLowerCase();

            for (World world : Bukkit.getWorlds()) {
                final String worldName = world.getName();
                if (worldName.toLowerCase().startsWith(typed)) {
                    if (toReturn == null) {
                        toReturn = new LinkedList<>();
                    }
                    toReturn.add(worldName);
                }
            }
        }

        return toReturn == null ? Collections.emptyList() : toReturn;
    }
}

