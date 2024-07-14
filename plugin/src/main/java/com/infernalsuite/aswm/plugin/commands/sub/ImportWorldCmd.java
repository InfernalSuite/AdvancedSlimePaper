package com.infernalsuite.aswm.plugin.commands.sub;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.infernalsuite.aswm.api.exceptions.InvalidWorldException;
import com.infernalsuite.aswm.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.aswm.api.exceptions.WorldLoadedException;
import com.infernalsuite.aswm.api.exceptions.WorldTooBigException;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.commands.parser.NamedSlimeLoader;
import com.infernalsuite.aswm.plugin.config.ConfigManager;
import com.infernalsuite.aswm.plugin.config.WorldData;
import com.infernalsuite.aswm.plugin.config.WorldsConfig;
import com.infernalsuite.aswm.plugin.util.ExecutorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.injection.RawArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ImportWorldCmd extends SlimeCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportWorldCmd.class);

    public ImportWorldCmd(CommandManager commandManager) {
        super(commandManager);
    }

    private final Cache<String, String[]> importCache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    @Command("swp|aswm|swm import <path-to-world> <data-source> [new-world-name]")
    @CommandDescription("Convert a world to the slime format and save it.")
    @Permission("swm.importworld")
    @RawArgs
    public CompletableFuture<Void> importWorld(CommandSender sender, String[] args,
                                               @Argument(value = "path-to-world") String pathToWorld,
                                               @Argument(value = "data-source") NamedSlimeLoader loader,
                                               @Argument(value = "new-world-name") String newWorldName) {
        File worldDir = new File(pathToWorld);

        if (!worldDir.exists() || !worldDir.isDirectory()) {
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("Path " + worldDir.getPath() + " does not point out to a valid world directory.")).color(NamedTextColor.RED)
            );
        }

        String[] oldArgs = importCache.getIfPresent(sender.getName());

        if (oldArgs != null) {
            importCache.invalidate(sender.getName());

            if (Arrays.equals(args, oldArgs)) { // Make sure it's exactly the same command
                String worldName = newWorldName == null ? worldDir.getName() : newWorldName;
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("Importing world " + worldDir.getName() + " into data source " + loader.slimeLoader() + "..."))
                );

                WorldsConfig config = ConfigManager.getWorldConfig();

                if (config.getWorlds().containsKey(worldName)) {
                    throw new MessageCommandException(COMMAND_PREFIX.append(
                            Component.text("There is already a world called " + worldName + " inside the worlds config file.")).color(NamedTextColor.RED)
                    );
                }

                return CompletableFuture.runAsync(() -> {
                    try {
                        long start = System.currentTimeMillis();
                        SlimeWorld world = asp.readVanillaWorld(worldDir, worldName, loader.slimeLoader());
                        asp.saveWorld(world);

                        ExecutorUtil.runSyncAndWait(plugin, () -> {
                            asp.loadWorld(world, true);
                        });

                        sender.sendMessage(COMMAND_PREFIX.append(
                                Component.text("World ").color(NamedTextColor.GREEN)
                                        .append(Component.text(worldName).color(NamedTextColor.YELLOW))
                                        .append(Component.text(" imported successfully in " + (System.currentTimeMillis() - start) + "ms.")).color(NamedTextColor.GREEN)
                        ));

                        WorldData worldData = new WorldData();
                        StringBuilder spawn = new StringBuilder();
                        for (String key : world.getPropertyMap().getProperties().keySet()) {
                            switch (key.toLowerCase()) {
                                case "spawnx" ->
                                        spawn.append(world.getPropertyMap().getValue(SlimeProperties.SPAWN_X)).append(", ");
                                case "spawny" ->
                                        spawn.append(world.getPropertyMap().getValue(SlimeProperties.SPAWN_Y)).append(", ");
                                case "spawnz" -> spawn.append(world.getPropertyMap().getValue(SlimeProperties.SPAWN_Z));
                                case "environment" ->
                                        worldData.setEnvironment(world.getPropertyMap().getValue(SlimeProperties.ENVIRONMENT));
                                case "difficulty" ->
                                        worldData.setDifficulty(world.getPropertyMap().getValue(SlimeProperties.DIFFICULTY).toLowerCase());
                                case "allowmonsters" ->
                                        worldData.setAllowMonsters(world.getPropertyMap().getValue(SlimeProperties.ALLOW_MONSTERS));
                                case "dragonbattle" ->
                                        worldData.setDragonBattle(world.getPropertyMap().getValue(SlimeProperties.DRAGON_BATTLE));
                                case "pvp" -> worldData.setPvp(world.getPropertyMap().getValue(SlimeProperties.PVP));
                                case "worldtype" ->
                                        worldData.setWorldType(world.getPropertyMap().getValue(SlimeProperties.WORLD_TYPE));
                                case "defaultbiome" ->
                                        worldData.setDefaultBiome(world.getPropertyMap().getValue(SlimeProperties.DEFAULT_BIOME));
                            }
                        }

                        worldData.setDataSource(loader.name());
                        worldData.setSpawn(spawn.toString().isEmpty() ? "0.5, 255, 0.5" : spawn.toString());
                        config.getWorlds().put(worldName, worldData);
                        config.save();

                    } catch (WorldAlreadyExistsException ex) {
                        throw new MessageCommandException(COMMAND_PREFIX.append(
                                Component.text("Data source " + loader + " already contains a world called " + worldName + ".")).color(NamedTextColor.RED)
                        );
                    } catch (InvalidWorldException ex) {
                        throw new MessageCommandException(COMMAND_PREFIX.append(
                                Component.text("Directory " + worldDir.getName() + " does not contain a valid Minecraft world.")).color(NamedTextColor.RED)
                        );
                    } catch (WorldLoadedException ex) {
                        throw new MessageCommandException(COMMAND_PREFIX.append(
                                Component.text("World " + worldDir.getName() + " is loaded on this server. Please unload it before importing it.")).color(NamedTextColor.RED)
                        );
                    } catch (WorldTooBigException ex) {
                        throw new MessageCommandException(COMMAND_PREFIX.append(
                                Component.text("Hey! Didn't you just read the warning? The Slime Format isn't meant for big worlds." +
                                               " The world you provided just breaks everything. Please, trim it by using the MCEdit tool and try again.")).color(NamedTextColor.RED)
                        );
                    } catch (IOException ex) {
                        LOGGER.error("Failed to import world {}:", worldDir.getName(), ex);
                        throw new MessageCommandException(COMMAND_PREFIX.append(
                                Component.text("Failed to import world " + worldName + ". Take a look at the server console for more information.")).color(NamedTextColor.RED)
                        );
                    }

                });
            } else {
                return CompletableFuture.completedFuture(null);
            }
        } else {
            sender.sendMessage(COMMAND_PREFIX.append(
                    Component.text("WARNING: ").color(NamedTextColor.RED)
                            .append(Component.text("The Slime Format is meant to be used on tiny maps, " +
                                                   "not big survival worlds. It is recommended to trim your world " +
                                                   "by using the Prune MCEdit tool to ensure you don't save more chunks than you want to."))
                            .append(Component.newline())
                            .append(Component.text("If you are sure you want to continue, type again this command.")).color(NamedTextColor.GRAY)
            ));

            importCache.put(sender.getName(), args);
            return CompletableFuture.completedFuture(null);
        }
    }

}

