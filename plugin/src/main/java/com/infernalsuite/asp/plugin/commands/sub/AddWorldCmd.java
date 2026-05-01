package com.infernalsuite.asp.plugin.commands.sub;


import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.plugin.commands.CommandManager;
import com.infernalsuite.asp.plugin.commands.SlimeCommand;
import com.infernalsuite.asp.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.asp.plugin.commands.parser.NamedSlimeLoader;
import com.infernalsuite.asp.plugin.config.ConfigManager;
import com.infernalsuite.asp.plugin.config.WorldData;
import com.infernalsuite.asp.plugin.config.WorldsConfig;
import com.infernalsuite.asp.plugin.util.ExecutorUtil;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.incendo.cloud.annotations.*;
import org.incendo.cloud.paper.util.sender.Source;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class AddWorldCmd extends SlimeCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AddWorldCmd.class);

    public AddWorldCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swp|aswm|swm add <world> <data-source>")
    @CommandDescription("Adds an already existing world from a datasource to the plugin")
    @Permission("swm.addworld")
    public CompletableFuture<Void> addWorld(
            Source sender,
            @Argument(value = "world") String worldName,
            @Argument(value = "data-source") NamedSlimeLoader loader
    ) {
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

        return CompletableFuture.runAsync(() -> {
            commandManager.getWorldsInUse().add(worldName);

            try {
                SlimeWorld loadedWorld = AdvancedSlimePaperAPI.instance().readWorld(loader.slimeLoader(), worldName, false, new SlimePropertyMap());

                long start = System.currentTimeMillis();

                WorldData worldData = new WorldData();
                worldData.setSpawn("0, 64, 0");
                worldData.setDataSource(loader.name());
                worldData.setDefaultBiome(loadedWorld.getPropertyMap().getValue(SlimeProperties.DEFAULT_BIOME));
                worldData.setEnvironment(loadedWorld.getPropertyMap().getValue(SlimeProperties.ENVIRONMENT));

                ExecutorUtil.runSyncAndWait(plugin, () -> {
                    try {
                        asp.loadWorld(loadedWorld, true);

                        // Config
                        config.getWorlds().put(worldName, worldData);
                    } catch (IllegalArgumentException ex) {
                        throw new MessageCommandException(COMMAND_PREFIX.append(
                                Component.text("Failed to add world " + worldName + ": " + ex.getMessage() + ".").color(NamedTextColor.RED)
                        ));
                    }
                });
                config.save();

                sender.source().sendMessage(COMMAND_PREFIX.append(
                        Component.text("World ").color(NamedTextColor.GREEN)
                                .append(Component.text(worldName).color(NamedTextColor.YELLOW))
                                .append(Component.text(" added in " + (System.currentTimeMillis() - start) + "ms!").color(NamedTextColor.GREEN))
                ));
            } catch (IOException | UnknownWorldException | CorruptedWorldException | NewerFormatException ex) {
                LOGGER.error("Failed to add world {}:", worldName, ex);
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to add world " + worldName + ". Take a look at the server console for more information.").color(NamedTextColor.RED)
                ));
            } finally {
                commandManager.getWorldsInUse().remove(worldName);
            }
        });
    }
}

