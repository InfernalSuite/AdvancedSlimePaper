package com.infernalsuite.aswm.plugin.commands;

import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.plugin.SWPlugin;
import com.infernalsuite.aswm.plugin.commands.exception.MessageCommandException;
import com.infernalsuite.aswm.plugin.commands.parser.*;
import com.infernalsuite.aswm.plugin.commands.parser.suggestion.KnownSlimeWorldSuggestionProvider;
import com.infernalsuite.aswm.plugin.commands.sub.*;
import io.leangen.geantyref.TypeToken;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.bukkit.CloudBukkitCapabilities;
import org.incendo.cloud.component.DefaultValue;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.exception.InvalidSyntaxException;
import org.incendo.cloud.exception.NoPermissionException;
import org.incendo.cloud.exception.handling.ExceptionHandler;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.minecraft.extras.MinecraftHelp;
import org.incendo.cloud.paper.LegacyPaperCommandManager;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.parser.ParserRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

import static org.incendo.cloud.parser.standard.StringParser.greedyStringParser;

public class CommandManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(CommandManager.class);

    // A list containing all the worlds that are being performed operations on, so two commands cannot be run at the same time
    private final Set<String> worldsInUse = new HashSet<>();

    private final SWPlugin plugin;

    public CommandManager(SWPlugin plugin) {

        LegacyPaperCommandManager<CommandSender> commandManager = LegacyPaperCommandManager.createNative(
                plugin,
                ExecutionCoordinator.coordinatorFor(ExecutionCoordinator.nonSchedulingExecutor())
        );
        this.plugin = plugin;

        if (commandManager.hasCapability(CloudBukkitCapabilities.NATIVE_BRIGADIER)) {
            commandManager.registerBrigadier();
        } else {
            LOGGER.warn("Brigadier is not supported on this server version."); // This should never happen since we use ASP, but just in case
        }

        ParserRegistry<CommandSender> parserRegistry = commandManager.parserRegistry();

        parserRegistry.registerSuggestionProvider("known-slime-worlds", new KnownSlimeWorldSuggestionProvider());

        parserRegistry.registerParserSupplier(TypeToken.get(NamedWorldData.class), parserParameters -> new NamedWorldDataParser());
        parserRegistry.registerParserSupplier(TypeToken.get(SlimeWorld.class), parserParameters -> new SlimeWorldParser());
        parserRegistry.registerParserSupplier(TypeToken.get(NamedSlimeLoader.class), parserParameters -> new NamedSlimeLoaderParser(plugin.getLoaderManager()));
        parserRegistry.registerParserSupplier(TypeToken.get(World.class), parserParameters -> new BukkitWorldParser());

        commandManager.exceptionController().registerHandler(TypeToken.get(CommandExecutionException.class), ExceptionHandler.unwrappingHandler()); // Unwrap the exception
        commandManager.exceptionController().registerHandler(TypeToken.get(ArgumentParseException.class), context -> {
            Throwable cause = context.exception().getCause();
            if (cause instanceof MessageCommandException message) {
                context.context().sender().sendMessage(message.getComponent());
            } else {
                String message = cause.getMessage();
                if (message == null) {
                    message = "An error occurred while parsing the command!";
                }

                context.context().sender().sendMessage(SlimeCommand.COMMAND_PREFIX.append(Component.text(message)).color(NamedTextColor.RED));
            }
        });
        commandManager.exceptionController().registerHandler(TypeToken.get(InvalidSyntaxException.class), context -> {
            InvalidSyntaxException e = context.exception();

            if (e.currentChain().size() == 1) {
                context.context().sender().sendMessage(SlimeCommand.COMMAND_PREFIX.append(
                        Component.text("Unknown subcommand. To check out help page, type ").color(NamedTextColor.RED)
                                .append(Component.text("/swm help").color(NamedTextColor.GRAY))
                                .append(Component.text(".")).color(NamedTextColor.RED)
                ));
            } else {
                context.context().sender().sendMessage(SlimeCommand.COMMAND_PREFIX.append(
                        Component.text("Command usage: ").color(NamedTextColor.RED)
                                .append(Component.text("/" + e.correctSyntax()).color(NamedTextColor.GRAY))
                                .append(Component.text(".")).color(NamedTextColor.RED)
                ));
            }
        });
        commandManager.exceptionController().registerHandler(TypeToken.get(NoPermissionException.class), context -> {
            context.context().sender().sendMessage(SlimeCommand.COMMAND_PREFIX.append(
                    Component.text("You do not have permission to perform this command.").color(NamedTextColor.RED)
            ));

        });
        commandManager.exceptionController().registerHandler(TypeToken.get(MessageCommandException.class), context -> {
            context.context().sender().sendMessage(context.exception().getComponent());
        });

        AnnotationParser<CommandSender> ap = new AnnotationParser<>(commandManager, CommandSender.class);

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
    public void onCommand(CommandSender sender) {
        sender.sendMessage(SlimeCommand.COMMAND_PREFIX.append(
                Component.text("This is the main command for the Slime World Plugin. Type ").color(NamedTextColor.GRAY)
                        .append(Component.text("/swp help").color(NamedTextColor.YELLOW))
                        .append(Component.text(" to see all available commands.")).color(NamedTextColor.GRAY)
        ));
    }

    SWPlugin getPlugin() {
        return plugin;
    }


}
