package com.infernalsuite.aswm.plugin.commands.sub;

import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.commands.parser.NamedSlimeLoader;
import com.infernalsuite.aswm.plugin.commands.parser.NamedWorldData;
import com.infernalsuite.aswm.plugin.config.ConfigManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.annotations.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class MigrateWorldCmd extends SlimeCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(MigrateWorldCmd.class);

    public MigrateWorldCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swp|aswm|swm migrate <world> <new-data-source>")
    @CommandDescription("Migrate a world from one data source to another.")
    @Permission("swm.migrate")
    public CompletableFuture<Void> onCommand(CommandSender sender, @Argument(value = "world") NamedWorldData worldData,
                                             @Argument(value = "new-data-source") NamedSlimeLoader newLoader) {
        String currentSource = worldData.worldData().getDataSource();

        if (newLoader.name().equalsIgnoreCase(currentSource)) {
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World " + worldData.name() + " is already stored using data source " + currentSource + "!").color(NamedTextColor.RED)
            ));
        }

        SlimeLoader oldLoader = plugin.getLoaderManager().getLoader(currentSource);

        if (oldLoader == null) {
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("Unknown data source " + currentSource + "! Are you sure you configured it correctly?").color(NamedTextColor.RED)
            ));
        }

        if (commandManager.getWorldsInUse().contains(worldData.name())) {
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World " + worldData.name() + " is already being used on another command! Wait some time and try again.").color(NamedTextColor.RED)
            ));
        }

        commandManager.getWorldsInUse().add(worldData.name());

        return CompletableFuture.runAsync(() -> {
            try {
                long start = System.currentTimeMillis();
                asp.migrateWorld(worldData.name(), oldLoader, newLoader.slimeLoader());

                worldData.worldData().setDataSource(newLoader.name());
                ConfigManager.getWorldConfig().save();

                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("World ").color(NamedTextColor.GRAY)
                                .append(Component.text(worldData.name()).color(NamedTextColor.YELLOW))
                                .append(Component.text(" migrated in ").color(NamedTextColor.GRAY))
                                .append(Component.text(System.currentTimeMillis() - start).color(NamedTextColor.YELLOW))
                                .append(Component.text("ms!").color(NamedTextColor.GRAY))
                ));
            } catch (IOException ex) {
                LOGGER.error("Failed to migrate world {} (using data sources {} and {}).", worldData.name(), currentSource, newLoader.name(), ex);
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to migrate world " + worldData.name() + " (using data sources " + currentSource + " and " + newLoader.name() + "). Take a look at the server console for more information.").color(NamedTextColor.RED)
                ));
            } catch (WorldAlreadyExistsException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Data source " + newLoader.name() + " already contains a world named " + worldData.name() + "!").color(NamedTextColor.RED)
                ));
            } catch (UnknownWorldException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Can't find world " + worldData.name() + " in data source " + currentSource + ".").color(NamedTextColor.RED)
                ));
            } finally {
                commandManager.getWorldsInUse().remove(worldData.name());
            }
        });
    }
}

