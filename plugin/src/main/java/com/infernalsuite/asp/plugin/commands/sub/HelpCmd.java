package com.infernalsuite.asp.plugin.commands.sub;

import net.kyori.adventure.audience.Audience;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.CommandDescription;
import org.incendo.cloud.minecraft.extras.AudienceProvider;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.Source;
import org.jetbrains.annotations.Nullable;

public class HelpCmd extends com.infernalsuite.asp.plugin.commands.SlimeCommand {

    private final MinecraftHelp<Source> help;

    public HelpCmd(com.infernalsuite.asp.plugin.commands.CommandManager commandManager, PaperCommandManager<Source> cloudCommandManager) {
        super(commandManager);
        this.help = MinecraftHelp.create("/swp help", cloudCommandManager, Source::source);
    }

    @Command("swp|aswm|swm help [query]")
    @CommandDescription("Displays the help message.")
    public void help(Source sender, @Argument(value = "query") @Nullable String[] query) {
        String parsedQuery = query == null ? "" : String.join(" ", query);
        help.queryCommands(parsedQuery, sender);
    }
}
