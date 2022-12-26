package com.grinderwolf.swm.plugin.commands.sub;

import com.grinderwolf.swm.api.utils.SlimeFormat;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.log.Logging;
import lombok.Getter;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.util.Collections;
import java.util.List;

@Getter
public class VersionCmd implements Subcommand {

    private final String usage = "version";
    private final String description = "Shows the plugin version.";

    @Override
    public boolean onCommand(CommandSender sender, String[] args) {
        sender.sendMessage(Logging.COMMAND_PREFIX + ChatColor.GRAY + "This server is running SWM " + ChatColor.YELLOW + "v" + SWMPlugin.getInstance()
                .getDescription().getVersion() + ChatColor.GRAY + ", which supports up to Slime Format " + ChatColor.AQUA + "v" + SlimeFormat.SLIME_VERSION + ChatColor.GRAY + ".");

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
