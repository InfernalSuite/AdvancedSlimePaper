package com.infernalsuite.asp.plugin.commands.parser;

import com.infernalsuite.asp.plugin.commands.SlimeCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.concurrent.CompletableFuture;

public class BukkitWorldParser implements ArgumentParser<Source, World> {
    @Override
    public @NonNull ArgumentParseResult<@NonNull World> parse(@NonNull CommandContext<@NonNull Source> commandContext, @NonNull CommandInput commandInput) {
        String input = commandInput.peekString();
        World loaded = Bukkit.getWorld(input);

        if (loaded == null) {
            return ArgumentParseResult.failure(new com.infernalsuite.asp.plugin.commands.exception.MessageCommandException(SlimeCommand.COMMAND_PREFIX.append(
                    Component.text("World " + input + " is not loaded!").color(NamedTextColor.RED)
            )));
        }
        commandInput.readString();
        return ArgumentParseResult.success(loaded);
    }

    @Override
    public @NonNull SuggestionProvider<Source> suggestionProvider() {
        return (context, input) -> CompletableFuture.supplyAsync(() ->
                Bukkit.getWorlds()
                        .stream()
                        .map(World::getName)
                        .map(Suggestion::suggestion)
                        .toList()
        );
    }
}
