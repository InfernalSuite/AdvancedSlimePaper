package com.infernalsuite.aswm.plugin.commands.sub;


import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.commands.parser.NamedWorldData;
import com.infernalsuite.aswm.plugin.util.ExecutorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class LoadWorldCmd extends SlimeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoadWorldCmd.class);

    public LoadWorldCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swp|aswm|swm load <world>")
    @CommandDescription("Load a world")
    @Permission("swm.loadworld")
    public CompletableFuture<Void> onCommand(CommandSender sender, @Argument(value = "world") NamedWorldData worldData) {
        World world = Bukkit.getWorld(worldData.name());

        if (world != null) {
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World " + worldData.name() + " is already loaded!").color(NamedTextColor.RED)
            ));
        }

        if (commandManager.getWorldsInUse().contains(worldData.name())) {
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World " + worldData.name() + " is already being used on another command! Wait some time and try again.").color(NamedTextColor.RED)
            ));
        }

        commandManager.getWorldsInUse().add(worldData.name());
        sender.sendMessage(COMMAND_PREFIX.append(
                Component.text("Loading world ").color(NamedTextColor.GRAY)
                        .append(Component.text(worldData.name()).color(NamedTextColor.YELLOW))
                        .append(Component.text("...")).color(NamedTextColor.GRAY)
        ));

        // It's best to load the world async, and then just go back to the server thread and add it to the world list
        return CompletableFuture.runAsync(() -> {

            try {
                // ATTEMPT TO LOAD WORLD
                long start = System.currentTimeMillis();
                SlimeLoader loader = plugin.getLoaderManager().getLoader(worldData.worldData().getDataSource());

                if (loader == null) {
                    throw new IllegalArgumentException("invalid data source " + worldData.worldData().getDataSource());
                }

                SlimeWorld slimeWorld = asp.readWorld(loader, worldData.name(), worldData.worldData().isReadOnly(), worldData.worldData().toPropertyMap());
                ExecutorUtil.runSyncAndWait(plugin, () -> {
                    try {
                        asp.loadWorld(slimeWorld, true);
                    } catch (IllegalArgumentException ex) {
                        throw new MessageCommandException(COMMAND_PREFIX.append(
                                Component.text("Failed to generate world " + worldData.name() + ": " + ex.getMessage()).color(NamedTextColor.RED)
                        ));
                    }

                    sender.sendMessage(COMMAND_PREFIX.append(
                            Component.text("World ").color(NamedTextColor.GREEN)
                                    .append(Component.text(worldData.name()).color(NamedTextColor.YELLOW))
                                    .append(Component.text(" loaded and generated in " + (System.currentTimeMillis() - start) + "ms!").color(NamedTextColor.GREEN)
                            )
                    ));
                });
            } catch (CorruptedWorldException ex) {
                LOGGER.error("Failed to load world {}: world seems to be corrupted.", worldData.name(), ex);
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world " + worldData.name() + ": world seems to be corrupted.").color(NamedTextColor.RED)
                ));
            } catch (NewerFormatException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world " + worldData.name() + ": this world was serialized with a newer version of the Slime Format (" + ex.getMessage() + ") that SWM cannot understand.").color(NamedTextColor.RED)
                ));
            } catch (UnknownWorldException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world " + worldData.name() + ": world could not be found (using data source '" + worldData.worldData().getDataSource() + "').").color(NamedTextColor.RED)
                ));
            } catch (IllegalArgumentException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world " + worldData.name() + ": " + ex.getMessage()).color(NamedTextColor.RED)
                ));
            } catch (IOException ex) {
                LOGGER.error("Failed to load world {}:", worldData.name(), ex);

                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world " + worldData.name() + ". Take a look at the server console for more information.").color(NamedTextColor.RED)
                ));
            } finally {
                commandManager.getWorldsInUse().add(worldData.name());
            }
        });
    }
}

