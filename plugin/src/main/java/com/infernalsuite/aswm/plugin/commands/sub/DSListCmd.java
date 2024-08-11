package com.infernalsuite.aswm.plugin.commands.sub;

import com.infernalsuite.aswm.api.SlimeNMSBridge;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.CommandManager;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.commands.parser.NamedSlimeLoader;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class DSListCmd extends SlimeCommand {

    private static final int MAX_ITEMS_PER_PAGE = 5;
    private static final Logger LOGGER = LoggerFactory.getLogger(DSListCmd.class);

    public DSListCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swp|aswm|swm dslist <data-source> [page]")
    @CommandDescription("List all worlds inside a data source.")
    @Permission("swm.dslist")
    public CompletableFuture<Void> listWorlds(CommandSender sender, @Argument(value = "data-source") NamedSlimeLoader namedLoader,
                             @Default("1") @Argument(value = "page") int page) {
        SlimeLoader loader = namedLoader.slimeLoader();

        if (page < 1) {
            throw new MessageCommandException(COMMAND_PREFIX.append(
                    Component.text("Page number must be greater than 0!").color(NamedTextColor.RED)
            ));
        }

        return CompletableFuture.runAsync(() -> {
            List<String> worldList;

            try {
                //FIXME: This should utilize proper pagination and not fetch all worlds at once
                worldList = loader.listWorlds();
            } catch (IOException ex) {
                LOGGER.error("Failed to load world list:", ex);
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("Failed to load world list. Take a look at the server console for more information.").color(NamedTextColor.RED)
                ));
            }

            if (worldList.isEmpty()) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("There are no worlds stored in data source " + namedLoader.name() + ".").color(NamedTextColor.RED)
                ));
            }

            int offset = (page - 1) * MAX_ITEMS_PER_PAGE;
            double d = worldList.size() / (double) MAX_ITEMS_PER_PAGE;
            int maxPages = ((int) d) + ((d > (int) d) ? 1 : 0);

            if (offset >= worldList.size()) {
                throw new MessageCommandException(COMMAND_PREFIX.append(
                        Component.text("There " + (maxPages == 1 ? "is" : "are")
                                       + " only " + maxPages + " page" + (maxPages == 1 ? "" : "s") + "!").color(NamedTextColor.RED)
                ));
            }

            worldList.sort(String::compareTo);
            sender.sendMessage(COMMAND_PREFIX.append(
                    Component.text("World list ").color(NamedTextColor.YELLOW)
                            .append(Component.text("[" + page + "/" + maxPages + "]").color(NamedTextColor.YELLOW))
                            .append(Component.text(":").color(NamedTextColor.GRAY))
            ));

            for (int i = offset; (i - offset) < MAX_ITEMS_PER_PAGE && i < worldList.size(); i++) {
                String world = worldList.get(i);
                sender.sendMessage(COMMAND_PREFIX.append(
                        Component.text(" - ").color(NamedTextColor.GRAY)
                                .append(isLoaded(loader, world)
                                        ? Component.text(world).color(NamedTextColor.GREEN)
                                        : Component.text(world).color(NamedTextColor.RED))
                ));
            }
        });
    }

    private boolean isLoaded(SlimeLoader loader, String worldName) {
        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            SlimeWorld slimeWorld = SlimeNMSBridge.instance().getInstance(world).getSlimeWorldMirror();

            if (slimeWorld != null) {
                return loader.equals(slimeWorld.getLoader());
            }
        }

        return false;
    }
}
