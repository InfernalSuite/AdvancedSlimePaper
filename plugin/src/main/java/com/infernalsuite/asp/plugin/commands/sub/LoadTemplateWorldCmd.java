package com.infernalsuite.asp.plugin.commands.sub;


import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.incendo.cloud.paper.util.sender.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class LoadTemplateWorldCmd extends com.infernalsuite.asp.plugin.commands.SlimeCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadTemplateWorldCmd.class);

    public LoadTemplateWorldCmd(com.infernalsuite.asp.plugin.commands.CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swp|aswm|swm load-template <template-world> <world-name>")
    @CommandDescription("Creates a temporary world using another as a template. This world will never be stored.")
    @Permission("swm.loadworld.template")
    public CompletableFuture<Void> onCommand(Source source, @Argument(value = "template-world") com.infernalsuite.asp.plugin.commands.parser.NamedWorldData templateWorldData,
                                             @Argument(value = "world-name") String worldName) {
        CommandSender sender = source.source();
        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            throw new com.infernalsuite.asp.plugin.commands.exception.MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World " + worldName + " is already loaded!").color(NamedTextColor.RED)
            ));
        }

        if (templateWorldData.name().equals(worldName)) {
            throw new com.infernalsuite.asp.plugin.commands.exception.MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("The template world name cannot be the same as the cloned world one!").color(NamedTextColor.RED)
            ));
        }

        if (commandManager.getWorldsInUse().contains(worldName)) {
            throw new com.infernalsuite.asp.plugin.commands.exception.MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World " + worldName + " is already being used on another command! Wait some time and try again.").color(NamedTextColor.RED)
            ));
        }

        commandManager.getWorldsInUse().add(worldName);
        sender.sendMessage(COMMAND_PREFIX.append(
                Component.text("Creating world ").color(NamedTextColor.GRAY)
                        .append(Component.text(worldName).color(NamedTextColor.YELLOW))
                        .append(Component.text(" using ").color(NamedTextColor.GRAY))
                        .append(Component.text(templateWorldData.name()).color(NamedTextColor.YELLOW))
                        .append(Component.text(" as a template...").color(NamedTextColor.GRAY)
                )
        ));

        // It's best to load the world async, and then just go back to the server thread and add it to the world list
        return CompletableFuture.runAsync(() -> {
            try {
                long start = System.currentTimeMillis();
                SlimeLoader loader = plugin.getLoaderManager().getLoader(templateWorldData.worldData().getDataSource());

                if (loader == null) {
                    throw new IllegalArgumentException("invalid data source " + templateWorldData.worldData().getDataSource());
                }

                SlimeWorld templateWorld = getWorldReadyForCloning(templateWorldData.name(), loader, templateWorldData.worldData().toPropertyMap());
                SlimeWorld slimeWorld = templateWorld.clone(worldName);
                com.infernalsuite.asp.plugin.util.ExecutorUtil.runSyncAndWait(plugin, () -> {
                    try {
                        asp.loadWorld(slimeWorld, true);
                    } catch (IllegalArgumentException ex) {
                        throw new com.infernalsuite.asp.plugin.commands.exception.MessageCommandException(COMMAND_PREFIX.append(
                                Component.text("Failed to generate world " + worldName + ": " + ex.getMessage() + ".").color(NamedTextColor.RED)
                        ));
                    }
                });
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("World ").color(NamedTextColor.GREEN)
                                .append(Component.text(worldName).color(NamedTextColor.YELLOW))
                                .append(Component.text(" loaded and generated in " + (System.currentTimeMillis() - start) + "ms!")).color(NamedTextColor.GREEN)
                ));
            } catch (CorruptedWorldException ex) {
                LOGGER.error("Failed to load world {}: world seems to be corrupted.", templateWorldData.name(), ex);
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("Failed to load world " + templateWorldData.name() + ": world seems to be corrupted.").color(NamedTextColor.RED)
                ));
            } catch (NewerFormatException ex) {
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("Failed to load world " + templateWorldData.name() + ": this world" +
                                " was serialized with a newer version of the Slime Format (" + ex.getMessage() + ") that SWM cannot understand.").color(NamedTextColor.RED)
                ));
            } catch (UnknownWorldException ex) {
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("Failed to load world " + templateWorldData.name() +
                                ": world could not be found (using data source '" + templateWorldData.worldData().getDataSource() + "').").color(NamedTextColor.RED)
                ));
            } catch (IllegalArgumentException ex) {
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("Failed to load world " + templateWorldData.name() +
                                ": " + ex.getMessage()).color(NamedTextColor.RED)
                ));
            } catch (IOException ex) {
                LOGGER.error("Failed to load world {}:", templateWorldData.name(), ex);
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("Failed to load world " + templateWorldData.name() +
                                ". Take a look at the server console for more information.").color(NamedTextColor.RED)
                ));
            } finally {
                commandManager.getWorldsInUse().remove(worldName);
            }
        });
    }
}

