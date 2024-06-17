package com.infernalsuite.aswm.plugin.commands.parser;

import com.infernalsuite.aswm.api.AdvancedSlimePaperAPI;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.plugin.commands.SlimeCommand;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
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

public class SlimeWorldParser implements ArgumentParser<CommandSender, SlimeWorld> {
    @Override
    public @NonNull ArgumentParseResult<@NonNull SlimeWorld> parse(@NonNull CommandContext<@NonNull CommandSender> commandContext, @NonNull CommandInput commandInput) {
        String input = commandInput.peekString();
        SlimeWorld loaded = AdvancedSlimePaperAPI.instance().getLoadedWorld(input);

        if (loaded == null) {
            return ArgumentParseResult.failure(new MessageCommandException(SlimeCommand.COMMAND_PREFIX.append(
                    Component.text("World " + input + " is not loaded!").color(NamedTextColor.RED)
            )));
        }
        commandInput.readString();
        return ArgumentParseResult.success(loaded);
    }

    @Override
    public @NonNull SuggestionProvider<CommandSender> suggestionProvider() {
        return (context, input) -> CompletableFuture.supplyAsync(() ->
                AdvancedSlimePaperAPI.instance().getLoadedWorlds()
                        .stream()
                        .map(SlimeWorld::getName)
                        .map(Suggestion::suggestion)
                        .toList()
        );
    }
}
