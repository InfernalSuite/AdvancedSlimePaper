package com.grinderwolf.swm.plugin.commands.sub;

import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.world.SlimeWorld;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.loaders.LoaderUtils;
import com.grinderwolf.swm.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@Getter
public class DSListCmd implements Subcommand {

    private static final int MAX_ITEMS_PER_PAGE = 5;

    private final String usage = "dslist <data-source> [page]";
    private final String description = "List all worlds inside a data source.";
    private final String permission = "swm.dslist";

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        if (args.length > 0) {
            int page;

            if (args.length == 1) {
                page = 1;
            } else {
                String pageString = args[1];

                try {
                    page = Integer.parseInt(pageString);

                    if (page < 1) {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException ex) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "'" + pageString + "' is not a valid number.");

                    return true;
                }
            }

            String source = args[0];
            SlimeLoader loader = LoaderUtils.getLoader(source);

            if (loader == null) {
                sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Unknown data source " + source + ".");

                return true;
            }

            Bukkit.getScheduler().runTaskAsynchronously(SWMPlugin.getInstance(), () -> {

                List<String> worldList;

                try {
                    worldList = loader.listWorlds();
                } catch (IOException ex) {
                    if (!(sender instanceof ConsoleCommandSender)) {
                        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "Failed to load world list. Take a look at the server console for more information.");
                    }

                    Logging.error("Failed to load world list:");
                    ex.printStackTrace();
                    return;
                }

                if (worldList.isEmpty()) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "There are no worlds stored in data source " + source + ".");

                    return;
                }

                int offset = (page - 1) * MAX_ITEMS_PER_PAGE;
                double d = worldList.size() / (double) MAX_ITEMS_PER_PAGE;
                int maxPages = ((int) d) + ((d > (int) d) ? 1 : 0);

                if (offset >= worldList.size()) {
                    sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.RED + "There " + (maxPages == 1 ? "is" :
                            "are") + " only " + maxPages + " page" + (maxPages == 1 ? "" : "s") + "!");

                    return;
                }

                worldList.sort(String::compareTo);
                sender.sendMessage(Logging.COMMAND_PREFIX + "World list " + ChatColor.YELLOW + "[" + page + "/" + maxPages + "]" + ChatColor.GRAY + ":");

                for (int i = offset; (i - offset) < MAX_ITEMS_PER_PAGE && i < worldList.size(); i++) {
                    String world = worldList.get(i);
                    sender.sendMessage(ChatColor.GRAY + " - " + (isLoaded(loader, world) ? ChatColor.GREEN : ChatColor.RED) + world);
                }

            });

            return true;
        }

        return false;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            return new LinkedList<>(LoaderUtils.getAvailableLoadersNames());
        }

        return Collections.emptyList();
    }

    private boolean isLoaded(SlimeLoader loader, String worldName) {
        World world = Bukkit.getWorld(worldName);

        if (world != null) {
            SlimeWorld slimeWorld = SWMPlugin.getInstance().getNms().getSlimeWorld(world);

            if (slimeWorld != null) {
                return loader.equals(slimeWorld.getLoader());
            }
        }

        return false;
    }
}
