package com.infernalsuite.asp;

import net.minecraft.server.MinecraftServer;
import org.bukkit.Server;
import org.bukkit.craftbukkit.scheduler.MinecraftInternalPlugin;
import org.bukkit.plugin.PluginLogger;
import org.jetbrains.annotations.NotNull;

import java.util.logging.LogRecord;

public class InternalPlugin extends MinecraftInternalPlugin {

    @Override
    public @NotNull Server getServer() {
        return MinecraftServer.getServer().server;
    }

    @Override
    public @NotNull PluginLogger getLogger() {
        return new PluginLogger(new InternalPlugin()) {
            @Override
            public void log(@NotNull LogRecord logRecord) {
                MinecraftServer.LOGGER.info(logRecord.getMessage());
            }
        };
    }

}
