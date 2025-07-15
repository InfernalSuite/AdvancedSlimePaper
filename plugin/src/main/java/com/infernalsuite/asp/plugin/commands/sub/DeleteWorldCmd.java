package com.infernalsuite.asp.plugin.commands.sub;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.annotations.injection.RawArgs;
import org.incendo.cloud.paper.util.sender.Source;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DeleteWorldCmd extends com.infernalsuite.asp.plugin.commands.SlimeCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeleteWorldCmd.class);

    public DeleteWorldCmd(com.infernalsuite.asp.plugin.commands.CommandManager commandManager) {
        super(commandManager);
    }

    private final Cache<String, String[]> deleteCache = CacheBuilder.newBuilder().expireAfterWrite(30, TimeUnit.SECONDS).build();

    @Command("swp|aswm|swm delete <world> [data-source]")
    @CommandDescription("Delete a world")
    @Permission("swm.deleteworld")
    @RawArgs
    public CompletableFuture<Void> deleteWorld(Source source, String[] args,
                                               @Argument(value = "world", suggestions = "known-slime-worlds") String worldName,
                                               @Argument(value = "data-source") @Nullable com.infernalsuite.asp.plugin.commands.parser.NamedSlimeLoader dataSource) {
        CommandSender sender = source.source();
        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            throw new com.infernalsuite.asp.plugin.commands.exception.MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World " + worldName + " is loaded on this server! Unload it by running the command ").color(NamedTextColor.RED)
                            .append(Component.text("/swm unload " + worldName).color(NamedTextColor.GRAY))
                            .append(Component.text(".")).color(NamedTextColor.RED)
            ));
        }

        SlimeLoader loader;

        if (dataSource != null) {
            loader = dataSource.slimeLoader();
        } else {
            com.infernalsuite.asp.plugin.config.WorldsConfig config = com.infernalsuite.asp.plugin.config.ConfigManager.getWorldConfig();
            com.infernalsuite.asp.plugin.config.WorldData worldData = config.getWorlds().get(worldName);

            if (worldData == null) {
                throw new com.infernalsuite.asp.plugin.commands.exception.MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to find world " + worldName + " inside the worlds config file!").color(NamedTextColor.RED)
                ));
            }

            loader = plugin.getLoaderManager().getLoader(worldData.getDataSource());
        }

        if (loader == null) {
            // This could happen if the loader inside WorldData is invalid
            throw new com.infernalsuite.asp.plugin.commands.exception.MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("Unknown data source! Are you sure you typed it correctly?").color(NamedTextColor.RED)
            ));
        }

        if (commandManager.getWorldsInUse().contains(worldName)) {
            throw new com.infernalsuite.asp.plugin.commands.exception.MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World " + worldName + " is already being used on another command! Wait some time and try again.").color(NamedTextColor.RED)
            ));
        }

        String[] oldArgs = deleteCache.getIfPresent(sender.getName());

        if (oldArgs != null) {
            deleteCache.invalidate(sender.getName());

            if (Arrays.equals(args, oldArgs)) { // Make sure it's exactly the same command
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("Deleting world ").color(NamedTextColor.GRAY)
                                .append(Component.text(worldName).color(NamedTextColor.YELLOW))
                                .append(Component.text("...")).color(NamedTextColor.GRAY)
                ));

                // No need to do this synchronously
                commandManager.getWorldsInUse().add(worldName);
                return CompletableFuture.runAsync(() -> {
                    try {
                        long start = System.currentTimeMillis();
                        loader.deleteWorld(worldName);

                        // Now let's delete it from the config file
                        com.infernalsuite.asp.plugin.config.WorldsConfig config = com.infernalsuite.asp.plugin.config.ConfigManager.getWorldConfig();

                        config.getWorlds().remove(worldName);
                        config.save();

                        sender.sendMessage(COMMAND_PREFIX.append(
                                Component.text("World ").color(NamedTextColor.GREEN)
                                        .append(Component.text(worldName).color(NamedTextColor.YELLOW))
                                        .append(Component.text(" deleted in " + (System.currentTimeMillis() - start) + "ms!").color(NamedTextColor.GREEN)
                                        )));
                    } catch (IOException ex) {
                        LOGGER.error("Failed to delete world {}", worldName, ex);

                        throw new com.infernalsuite.asp.plugin.commands.exception.MessageCommandException(COMMAND_PREFIX.append(
                                Component.text("Failed to delete world " + worldName + ". Take a look at the server console for more information.").color(NamedTextColor.RED)
                        ));
                    } catch (UnknownWorldException ex) {
                        throw new com.infernalsuite.asp.plugin.commands.exception.MessageCommandException(COMMAND_PREFIX.append(
                                Component.text("Data source " + loader + " does not contain any world called " + worldName + ".").color(NamedTextColor.RED)
                        ));
                    } finally {
                        commandManager.getWorldsInUse().remove(worldName);
                    }
                });
            } else {
                return CompletableFuture.completedFuture(null);
            }
        } else {
            return CompletableFuture.runAsync(() -> {
                deleteCache.put(sender.getName(), args);
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("WARNING: ").color(NamedTextColor.RED)
                                .append(Component.text("You're about to delete world ").color(NamedTextColor.GRAY))
                                .append(Component.text(worldName).color(NamedTextColor.YELLOW))
                                .append(Component.text(". This action cannot be undone.").color(NamedTextColor.GRAY))
                                .append(Component.newline())
                                .append(Component.text("If you are sure you want to continue, type again this command.").color(NamedTextColor.GRAY)
                                )));
            });
        }
    }
}

