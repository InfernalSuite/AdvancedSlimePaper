package com.infernalsuite.asp.plugin.commands.parser.suggestion;

import com.infernalsuite.asp.plugin.config.ConfigManager;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.concurrent.CompletableFuture;

public class KnownSlimeWorldSuggestionProvider implements SuggestionProvider<Source> {
    @Override
    public @NonNull CompletableFuture<? extends @NonNull Iterable<? extends @NonNull Suggestion>> suggestionsFuture(@NonNull CommandContext<Source> context, @NonNull CommandInput input) {
        return CompletableFuture.supplyAsync(() ->
                ConfigManager.getWorldConfig()
                        .getWorlds()
                        .keySet()
                        .stream()
                        .map(Suggestion::suggestion)
                        .toList()
        );
    }
}
