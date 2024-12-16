package com.infernalsuite.asp.plugin.commands.sub;

import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.paper.PaperCommandManager;
import org.jetbrains.annotations.Nullable;

public class HelpCmd extends com.infernalsuite.asp.plugin.commands.SlimeCommand {

    private final MinecraftHelp<CommandSender> help;

    public HelpCmd(com.infernalsuite.asp.plugin.commands.CommandManager commandManager, LegacyPaperCommandManager<CommandSender> cloudCommandManager) {
        super(commandManager);
        this.help = MinecraftHelp.createNative("/swp help", cloudCommandManager);
    }

    @Command("swp|aswm|swm help [query]")
    @CommandDescription("Displays the help message.")
    public void help(CommandSender sender, @Argument(value = "query") @Nullable String[] query) {
        String parsedQuery = query == null ? "" : String.join(" ", query);
        help.queryCommands(parsedQuery, sender);
    }
}
