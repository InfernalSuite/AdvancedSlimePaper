package com.infernalsuite.asp.plugin.commands;

import com.infernalsuite.asp.api.AdvancedSlimePaperAPI;
import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.exceptions.UnknownWorldException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.IOException;

public class SlimeCommand {
    public static final TextComponent COMMAND_PREFIX = LegacyComponentSerializer.legacySection().deserialize(
            "§9§lSWP §7§l>> §r"
    );

    protected final CommandManager commandManager;
    protected final com.infernalsuite.asp.plugin.SWPlugin plugin;
    protected final AdvancedSlimePaperAPI asp = AdvancedSlimePaperAPI.instance();

    public SlimeCommand(CommandManager commandManager) {
        this.commandManager = commandManager;
        this.plugin = commandManager.getPlugin();
    }

    // This method is here so that we can easily change the behavior in the future
    protected SlimeWorld getWorldReadyForCloning(String name, SlimeLoader loader, SlimePropertyMap propertyMap) throws CorruptedWorldException, NewerFormatException, UnknownWorldException, IOException {
        return asp.readWorld(loader, name, false, propertyMap);
    }
}
