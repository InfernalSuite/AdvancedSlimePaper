package com.grinderwolf.swm.plugin.Utils;

import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.commands.CommandManager;
import com.grinderwolf.swm.plugin.config.ConfigManager;
import com.grinderwolf.swm.plugin.config.WorldData;
import com.grinderwolf.swm.plugin.config.WorldsConfig;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.log.Logging;
import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.exceptions.WorldLockedException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class WorldManager {


    public static void setBlock(World world, int x,int y, int z, Material block){
        Location location = new Location(world,x,y,z);
        location.getBlock().setType(block);
    }

    //World Loading System\\
    /////////////////////##MODIFIERS##\\\\\\\\\\\\\\\\\\\\\
    public static boolean loadWorld(String worldName) {
        String[] args = new String[] {worldName};
       return (loadWorld(args));
    }
    /////////////////////##MODIFIERS##\\\\\\\\\\\\\\\\\\\\\

    public static boolean loadWorld(String[] args) {

        if (args.length > 0) {
            String worldName = args[0];
            World world = Bukkit.getWorld(worldName);

            if (world != null) {
                Logging.error("World " + worldName + " is already loaded!");

                return true;
            }

            WorldsConfig config = ConfigManager.getWorldConfig();
            WorldData worldData = config.getWorlds().get(worldName);

            if (worldData == null) {
                Logging.error("Failed to find world " + worldName + " inside the worlds config file.");

                return true;
            }

            if (CommandManager.getInstance().getWorldsInUse().contains(worldName)) {
                Logging.error("World " + worldName + " is already being used on another command! Wait some time and try again.");

                return true;
            }

            CommandManager.getInstance().getWorldsInUse().add(worldName);
            Logging.warning("Loading world " + ChatColor.GREEN + worldName + ChatColor.YELLOW + "...");

            // It's best to load the world async, and then just go back to the server thread and add it to the world list
            Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {

                try {
                    // ATTEMPT TO LOAD WORLD
                    long start = System.currentTimeMillis();
                    SlimeLoader loader = SWMPlugin.getInstance().getLoader(worldData.getDataSource());

                    if (loader == null) {
                        throw new IllegalArgumentException("invalid data source " + worldData.getDataSource());
                    }

                    SlimeWorld slimeWorld = SWMPlugin.getInstance().loadWorld(loader, worldName, worldData.isReadOnly(), worldData.toPropertyMap());
                    Bukkit.getScheduler().runTask(SWMPlugin.getInstance(), () -> {
                        try {
                            SWMPlugin.getInstance().loadWorld(slimeWorld, true);
                        } catch (IllegalArgumentException ex) {
                            Logging.error("Failed to generate world " + worldName + ": " + ex.getMessage() + ".");

                            return;
                        } catch (WorldLockedException | UnknownWorldException | NullPointerException | IOException exception) {
                            SWMPlugin.getInstance().getLogger().info("Failed to load world " + worldName + ": " + exception.getMessage());
                            Logging.error("Failed to load world " + worldName + ": " + exception.getMessage() + ".");
                            return;
                        }

                        Logging.success("World " + ChatColor.YELLOW + worldName
                                + ChatColor.GREEN + " loaded and generated in " + (System.currentTimeMillis() - start) + "ms!");
                    });
                } catch (CorruptedWorldException ex) {
                    Logging.error("Failed to load world " + worldName + ": world seems to be corrupted.");
                    ex.printStackTrace();
                } catch (NewerFormatException ex) {
                    Logging.error("Failed to load world " + worldName + ": this world" +
                            " was serialized with a newer version of the Slime Format (" + ex.getMessage() + ") that SWM cannot understand.");
                } catch (UnknownWorldException ex) {
                    Logging.error("Failed to load world " + worldName +
                            ": world could not be found (using data source '" + worldData.getDataSource() + "').");
                } catch (IllegalArgumentException ex) {
                    Logging.error("Failed to load world " + worldName +
                            ": " + ex.getMessage());
                } catch (IOException ex) {
                    Logging.error("Failed to load world " + worldName + ":");
                    ex.printStackTrace();
                } catch (WorldLockedException ex) {
                    Logging.error("Failed to load world " + worldName +
                            ": world is already in use. If you think this is a mistake, please wait some time and try again.");
                } finally {
                    CommandManager.getInstance().getWorldsInUse().remove(worldName);
                }
            });
            return true;
        }
        return false;
    }
    public static boolean unloadWorld(World world) {
        String[] args = new String[] {world.getName()};
        return (unloadWorld(args));
    }
    public static boolean unloadWorld(String worldName) {
        String[] args = new String[] {worldName};
        return (unloadWorld(args));
    }
    //World Unloading System\\
    public static boolean unloadWorld(String[] args) {

        if (args.length == 0) {
            return false;
        }


        var world = Bukkit.getWorld(args[0]);
        var worldName = args[0];

        if (world == null) {
            Logging.error("World " + worldName + " is not loaded!");

            return true;
        }

        String[] bannedWorlds = {"world", "world_nether", "world_the_end"};
     for (int i = 0; i <= bannedWorlds.length; i++) {
         if (!worldName.toLowerCase().equals(bannedWorlds[i].toLowerCase())) {
             break;
         } else {
             Logging.error("The world named: " + worldName + " is BANNED");
             return true;
         }
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
                    Logging.error("Failed to unload world " + worldName + ".");
                } else {
                    world.save();
                }
                unlockWorld(world, loader);
            });
        } else {
            Bukkit.unloadWorld(world, true);
            unlockWorld(world, loader);
        }
        return true;
    }

    //World Unlocker\\
    private static void unlockWorld(World world, SlimeLoader loader) {
        String worldName = world.getName();
        Logging.warning("Attempting to unlock world.. " + worldName + ".");
        try {
            if (loader != null && loader.isWorldLocked(worldName)) {
                Logging.warning("World.. " + worldName + " is locked.");
                loader.unlockWorld(worldName);
                Logging.warning("Attempted to unlock world.. " + worldName + ".");
            } else {
                Logging.error(worldName + " was not unlocked. This could be because the world is either unlocked or not in the config. This is not an error");
            }
        } catch (UnknownWorldException | IOException e) {
            e.printStackTrace();
        }
        Logging.success("World " + ChatColor.YELLOW + worldName + ChatColor.GREEN + " unloaded correctly.");
    }


    public static void unlockWorld(SlimeWorld world) {
        try {
            if (world.getLoader() != null) {
                world.getLoader().unlockWorld(world.getName());
            }
        } catch (IOException ex) {
            Logging.error("Failed to unlock world " + world.getName() + ". Retrying in 5 seconds. Stack trace:");
            ex.printStackTrace();

            Bukkit.getScheduler().runTaskLaterAsynchronously(SWMPlugin.getInstance(), () -> unlockWorld(world), 100);
        } catch (UnknownWorldException e) {
        }
    }

    //Find Deafult Spawn for the world\\
    @NotNull
    public static Location findValidDefaultSpawn() {
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
}
