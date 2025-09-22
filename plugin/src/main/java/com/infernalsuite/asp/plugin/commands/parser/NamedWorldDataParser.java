package com.infernalsuite.asp.plugin.commands.parser;

import com.infernalsuite.asp.plugin.commands.SlimeCommand;
import com.infernalsuite.asp.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.asp.plugin.commands.parser.suggestion.KnownSlimeWorldSuggestionProvider;
import com.infernalsuite.asp.plugin.config.ConfigManager;
import com.infernalsuite.asp.plugin.config.WorldData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.parser.ArgumentParseResult;
import org.incendo.cloud.parser.ArgumentParser;
import org.incendo.cloud.suggestion.SuggestionProvider;

public class NamedWorldDataParser implements ArgumentParser<Source, NamedWorldData> {

    @Override
    public @NonNull ArgumentParseResult<@NonNull NamedWorldData> parse(@NonNull CommandContext<@NonNull Source> commandContext, @NonNull CommandInput commandInput) {
        String input = commandInput.peekString();
        WorldData worldData = ConfigManager.getWorldConfig().getWorlds().get(input);

        if (worldData == null) {
            return ArgumentParseResult.failure(new MessageCommandException(SlimeCommand.COMMAND_PREFIX.append(
                    Component.text("Failed to find world " + input + "inside the worlds config file!").color(NamedTextColor.RED)
            )));
        }
        commandInput.readString();
        return ArgumentParseResult.success(new NamedWorldData(input, worldData));
    }

    @Override
    public @NonNull SuggestionProvider<Source> suggestionProvider() {
        return new KnownSlimeWorldSuggestionProvider();
    }
}
