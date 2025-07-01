package com.infernalsuite.asp.plugin.commands.sub;

import com.infernalsuite.asp.plugin.SWPlugin;
import com.infernalsuite.asp.api.utils.SlimeFormat;
import com.infernalsuite.asp.plugin.commands.CommandManager;
import com.infernalsuite.asp.plugin.commands.SlimeCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.paper.util.sender.Source;

public class VersionCmd extends SlimeCommand {

    public VersionCmd(CommandManager commandManager) {
        super(commandManager);
    }

    @Command("swp|aswm|swm version")
    @CommandDescription("Shows the plugin version.")
    public void showVersion(Source sender) {
        sender.source().sendMessage(COMMAND_PREFIX.append(
                Component.text("This server is running SWM ").color(NamedTextColor.GRAY)
                        .append(Component.text("v" + SWPlugin.getInstance().getDescription().getVersion()).color(NamedTextColor.YELLOW))
                        .append(Component.text(", which supports up to Slime Format ").color(NamedTextColor.GRAY))
                        .append(Component.text("v" + SlimeFormat.SLIME_VERSION).color(NamedTextColor.AQUA))
                        .append(Component.text(".").color(NamedTextColor.GRAY))
        ));
    }
}
