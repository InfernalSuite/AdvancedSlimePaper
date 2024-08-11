package com.infernalsuite.aswm.plugin.commands.sub;


import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.commands.parser.NamedSlimeLoader;
import com.infernalsuite.aswm.plugin.config.ConfigManager;
import com.infernalsuite.aswm.plugin.config.WorldData;
import com.infernalsuite.aswm.plugin.config.WorldsConfig;
import com.infernalsuite.aswm.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.plugin.util.ExecutorUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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

public class CreateWorldCmd extends SlimeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(CreateWorldCmd.class);

    public CreateWorldCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swp|aswm|swm create <world> <data-source>")
    @CommandDescription("Create an empty world")
    @Permission("swm.createworld")
    public CompletableFuture<Void> createWorld(CommandSender sender, @Argument(value = "world") String worldName,
                                         @Argument(value = "data-source") NamedSlimeLoader loader) {

        if (commandManager.getWorldsInUse().contains(worldName)) {
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World " + worldName + " is already being used on another command! Wait some time and try again.")).color(NamedTextColor.RED)
            );
        }

        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("World " + worldName + " already exists!")).color(NamedTextColor.RED)
            );
        }

        WorldsConfig config = ConfigManager.getWorldConfig();

        if (config.getWorlds().containsKey(worldName)) {
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("There is already a world called " + worldName + " inside the worlds config file.")).color(NamedTextColor.RED)
            );
        }

        commandManager.getWorldsInUse().add(worldName);
        sender.sendMessage(COMMAND_PREFIX.append(
                Component.text("Creating empty world ").color(NamedTextColor.GRAY)
                        .append(Component.text(worldName).color(NamedTextColor.YELLOW))
                        .append(Component.text("...")).color(NamedTextColor.GRAY)
        ));

        // It's best to create the world async, and then just go back to the server thread and add it to the world list
        return CompletableFuture.runAsync(() -> {

            try {
                long start = System.currentTimeMillis();

                if (loader.slimeLoader().worldExists(worldName)) {
                    throw new WorldAlreadyExistsException("World already exists");
                }

                WorldData worldData = new WorldData();
                worldData.setSpawn("0, 64, 0");
                worldData.setDataSource(loader.name());

                SlimePropertyMap propertyMap = worldData.toPropertyMap();
                SlimeWorld slimeWorld = asp.createEmptyWorld(worldName, false, propertyMap, loader.slimeLoader());
                asp.saveWorld(slimeWorld);

                ExecutorUtil.runSyncAndWait(plugin, () -> {
                    try {
                        asp.loadWorld(slimeWorld, true);

                        // Bedrock block
                        Location location = new Location(Bukkit.getWorld(worldName), 0, 61, 0);
                        location.getBlock().setType(Material.BEDROCK);

                        // Config
                        config.getWorlds().put(worldName, worldData);
                    } catch (IllegalArgumentException ex) {
                        throw new MessageCommandException(COMMAND_PREFIX.append(
                                Component.text("Failed to create world " + worldName + ": " + ex.getMessage() + ".").color(NamedTextColor.RED)
                        ));
                    }
                });
                config.save();

                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text("World ").color(NamedTextColor.GREEN)
                                .append(Component.text(worldName).color(NamedTextColor.YELLOW))
                                .append(Component.text(" created in " + (System.currentTimeMillis() - start) + "ms!").color(NamedTextColor.GREEN))
                ));
            } catch (WorldAlreadyExistsException ex) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to create world " + worldName + ": world already exists (using data source '" + loader.name() + "').").color(NamedTextColor.RED)
                ));
            } catch (IOException ex) {
                LOGGER.error("Failed to create world {}:", worldName, ex);
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to create world " + worldName + ". Take a look at the server console for more information.").color(NamedTextColor.RED)
                ));
            } finally {
                commandManager.getWorldsInUse().remove(worldName);
            }
        });
    }
}

