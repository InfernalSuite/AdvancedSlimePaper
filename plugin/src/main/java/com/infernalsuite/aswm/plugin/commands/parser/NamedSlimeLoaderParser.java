package com.infernalsuite.aswm.plugin.commands.parser;

import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.loader.LoaderManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.incendo.cloud.suggestion.SuggestionProvider;

import java.util.concurrent.CompletableFuture;

public class NamedSlimeLoaderParser implements ArgumentParser<CommandSender, NamedSlimeLoader> {

    private final LoaderManager loaderManager;

    public NamedSlimeLoaderParser(LoaderManager loaderManager) {
        this.loaderManager = loaderManager;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull NamedSlimeLoader> parse(@NonNull CommandContext<@NonNull CommandSender> commandContext, @NonNull CommandInput commandInput) {
        String input = commandInput.peekString();
        SlimeLoader loader = loaderManager.getLoader(input);

        if (loader == null) {
            return ArgumentParseResult.failure(new MessageCommandException(SlimeCommand.COMMAND_PREFIX.append(
                    Component.text("Unknown data source " + input + "!").color(NamedTextColor.RED)
            )));
        }
        commandInput.readString();
        return ArgumentParseResult.success(new NamedSlimeLoader(input, loader));
    }

    @Override
    public @NonNull SuggestionProvider<CommandSender> suggestionProvider() {
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
