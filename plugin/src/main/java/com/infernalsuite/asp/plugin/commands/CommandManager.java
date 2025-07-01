package com.infernalsuite.asp.plugin.commands;

import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.plugin.SWPlugin;
import com.infernalsuite.asp.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.asp.plugin.commands.parser.BukkitWorldParser;
import com.infernalsuite.asp.plugin.commands.parser.NamedSlimeLoader;
import com.infernalsuite.asp.plugin.commands.parser.NamedSlimeLoaderParser;
import com.infernalsuite.asp.plugin.commands.parser.NamedWorldData;
import com.infernalsuite.asp.plugin.commands.parser.NamedWorldDataParser;
import com.infernalsuite.asp.plugin.commands.parser.SlimeWorldParser;
import com.infernalsuite.asp.plugin.commands.parser.suggestion.KnownSlimeWorldSuggestionProvider;
import com.infernalsuite.asp.plugin.commands.sub.CloneWorldCmd;
import com.infernalsuite.asp.plugin.commands.sub.CreateWorldCmd;
import com.infernalsuite.asp.plugin.commands.sub.DSListCmd;
import com.infernalsuite.asp.plugin.commands.sub.DeleteWorldCmd;
import com.infernalsuite.asp.plugin.commands.sub.GotoCmd;
import com.infernalsuite.asp.plugin.commands.sub.HelpCmd;
import com.infernalsuite.asp.plugin.commands.sub.ImportWorldCmd;
import com.infernalsuite.asp.plugin.commands.sub.LoadTemplateWorldCmd;
import com.infernalsuite.asp.plugin.commands.sub.LoadWorldCmd;
import com.infernalsuite.asp.plugin.commands.sub.MigrateWorldCmd;
import com.infernalsuite.asp.plugin.commands.sub.ReloadConfigCmd;
import com.infernalsuite.asp.plugin.commands.sub.SaveWorldCmd;
import com.infernalsuite.asp.plugin.commands.sub.SetSpawnCmd;
import com.infernalsuite.asp.plugin.commands.sub.UnloadWorldCmd;
import com.infernalsuite.asp.plugin.commands.sub.VersionCmd;
import com.infernalsuite.asp.plugin.commands.sub.WorldListCmd;
import io.leangen.geantyref.TypeToken;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.exception.NoPermissionException;
import org.incendo.cloud.exception.handling.ExceptionHandler;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.paper.util.sender.PaperSimpleSenderMapper;
import org.incendo.cloud.paper.util.sender.Source;
import org.incendo.cloud.parser.ParserRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public class CommandManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandManager.class);

    // A list containing all the worlds that are being performed operations on, so two commands cannot be run at the same time
    private final Set<String> worldsInUse = new HashSet<>();

    private final SWPlugin plugin;

    public CommandManager(SWPlugin plugin) {

        PaperCommandManager<Source> commandManager = PaperCommandManager.builder(PaperSimpleSenderMapper.simpleSenderMapper())
                .executionCoordinator(ExecutionCoordinator.coordinatorFor(ExecutionCoordinator.nonSchedulingExecutor()))
                .buildOnEnable(plugin);

        this.plugin = plugin;

        ParserRegistry<Source> parserRegistry = commandManager.parserRegistry();

        parserRegistry.registerSuggestionProvider("known-slime-worlds", new KnownSlimeWorldSuggestionProvider());

        parserRegistry.registerParserSupplier(TypeToken.get(NamedWorldData.class), parserParameters -> new NamedWorldDataParser());
        parserRegistry.registerParserSupplier(TypeToken.get(SlimeWorld.class), parserParameters -> new SlimeWorldParser());
        parserRegistry.registerParserSupplier(TypeToken.get(NamedSlimeLoader.class), parserParameters -> new NamedSlimeLoaderParser(plugin.getLoaderManager()));
        parserRegistry.registerParserSupplier(TypeToken.get(World.class), parserParameters -> new BukkitWorldParser());

        commandManager.exceptionController().registerHandler(TypeToken.get(CommandExecutionException.class), ExceptionHandler.unwrappingHandler()); // Unwrap the exception
        commandManager.exceptionController().registerHandler(TypeToken.get(ArgumentParseException.class), context -> {
            Throwable cause = context.exception().getCause();
            if (cause instanceof MessageCommandException message) {
                context.context().sender().source().sendMessage(message.getComponent());
            } else {
                String message = cause.getMessage();
                if (message == null) {
                    message = "An error occurred while parsing the command!";
                }

                context.context().sender().source().sendMessage(SlimeCommand.COMMAND_PREFIX.append(Component.text(message)).color(NamedTextColor.RED));
            }
        });
        commandManager.exceptionController().registerHandler(TypeToken.get(InvalidSyntaxException.class), context -> {
            InvalidSyntaxException e = context.exception();

            if (e.currentChain().size() == 1) {
                context.context().sender().source().sendMessage(SlimeCommand.COMMAND_PREFIX.append(
                        Component.text("Unknown subcommand. To check out help page, type ").color(NamedTextColor.RED)
                                .append(Component.text("/swm help").color(NamedTextColor.GRAY))
                                .append(Component.text(".")).color(NamedTextColor.RED)
                ));
            } else {
                context.context().sender().source().sendMessage(SlimeCommand.COMMAND_PREFIX.append(
                        Component.text("Command usage: ").color(NamedTextColor.RED)
                                .append(Component.text("/" + e.correctSyntax()).color(NamedTextColor.GRAY))
                                .append(Component.text(".")).color(NamedTextColor.RED)
                ));
            }
        });
        commandManager.exceptionController().registerHandler(TypeToken.get(NoPermissionException.class), context -> {
            context.context().sender().source().sendMessage(SlimeCommand.COMMAND_PREFIX.append(
                    Component.text("You do not have permission to perform this command.").color(NamedTextColor.RED)
            ));

        });
        commandManager.exceptionController().registerHandler(TypeToken.get(MessageCommandException.class), context -> {
            context.context().sender().source().sendMessage(context.exception().getComponent());
        });

        AnnotationParser<Source> ap = new AnnotationParser<>(commandManager, Source.class);

        ap.parse(this,
                new CloneWorldCmd(this),
                new CreateWorldCmd(this),
                new DeleteWorldCmd(this),
                new DSListCmd(this),
                new GotoCmd(this),
                new ImportWorldCmd(this),
                new LoadTemplateWorldCmd(this),
                new LoadWorldCmd(this),
                new MigrateWorldCmd(this),
                new ReloadConfigCmd(this),
                new SaveWorldCmd(this),
                new SetSpawnCmd(this),
                new UnloadWorldCmd(this),
                new VersionCmd(this),
                new WorldListCmd(this),
                new HelpCmd(this, commandManager)
        );

    }

    public Set<String> getWorldsInUse() {
        return worldsInUse;
    }

    @Command("swp|aswm|swm")
    public void onCommand(Source sender) {
        sender.source().sendMessage(SlimeCommand.COMMAND_PREFIX.append(
                Component.text("This is the main command for the Slime World Plugin. Type ").color(NamedTextColor.GRAY)
                        .append(Component.text("/swp help").color(NamedTextColor.YELLOW))
                        .append(Component.text(" to see all available commands.")).color(NamedTextColor.GRAY)
        ));
    }

    SWPlugin getPlugin() {
        return plugin;
    }


}
