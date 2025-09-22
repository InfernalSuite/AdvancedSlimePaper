package com.infernalsuite.asp.plugin.commands.parser;

import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.plugin.commands.SlimeCommand;
import com.infernalsuite.asp.plugin.loader.LoaderManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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

public class NamedSlimeLoaderParser implements ArgumentParser<Source, NamedSlimeLoader> {

    private final LoaderManager loaderManager;

    public NamedSlimeLoaderParser(LoaderManager loaderManager) {
        this.loaderManager = loaderManager;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull NamedSlimeLoader> parse(@NonNull CommandContext<@NonNull Source> commandContext, @NonNull CommandInput commandInput) {
        String input = commandInput.peekString();
        SlimeLoader loader = loaderManager.getLoader(input);

        if (loader == null) {
            return ArgumentParseResult.failure(new com.infernalsuite.asp.plugin.commands.exception.MessageCommandException(SlimeCommand.COMMAND_PREFIX.append(
                    Component.text("Unknown data source " + input + "!").color(NamedTextColor.RED)
            )));
        }
        commandInput.readString();
        return ArgumentParseResult.success(new NamedSlimeLoader(input, loader));
    }

    @Override
    public @NonNull SuggestionProvider<Source> suggestionProvider() {
        return (commandContext, commandInput) -> CompletableFuture.supplyAsync(() ->
                loaderManager
                        .getLoaders()
                        .keySet()
                        .stream()
                        .map(Suggestion::suggestion)
                        .toList()
        );
    }
}
