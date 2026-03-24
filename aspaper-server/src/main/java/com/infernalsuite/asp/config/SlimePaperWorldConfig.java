package com.infernalsuite.asp.config;

import io.papermc.paper.configuration.Configurations;
import io.papermc.paper.configuration.PaperConfigurations;
import io.papermc.paper.configuration.WorldConfiguration;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRules;
import org.spigotmc.SpigotWorldConfig;

import java.nio.file.Path;

public class SlimePaperWorldConfig {

    private static final Identifier FAKE_WORLD_KEY = Identifier.fromNamespaceAndPath("infernalsuite", "asp-slimeworld");
    public static WorldConfiguration cachedSlimeWorldConfig;

    private SlimePaperWorldConfig() {}

    public static WorldConfiguration initializeOrGet() {
        if(cachedSlimeWorldConfig != null)
            return cachedSlimeWorldConfig;


        initialize(MinecraftServer.getServer().paperConfigurations, MinecraftServer.getServer());
        return cachedSlimeWorldConfig;
    }

    private static void initialize(
            PaperConfigurations paperConfigurations,
            MinecraftServer server
    ) {
        SpigotWorldConfig spigotWorldConfig = new SpigotWorldConfig("asp-slimeworld");

        GameRules gameRules = new GameRules(server.worldLoaderContext.dataConfiguration().enabledFeatures());

        Configurations.ContextMap contextMap = PaperConfigurations.createWorldContextMap(
                /*
                 * This might break if Paper team decides to extend/edit the functionality of this the world path property.
                 * But the goal is for paper to treat the supplied folder as a world folder and create a paper-world.yml file there.
                 *
                 * Users can edit this file to change the config for all slime worlds.
                 */
                Path.of("config", "advancedslimepaper"),

                "asp-slimeworld",
                FAKE_WORLD_KEY,
                spigotWorldConfig,
                server.registryAccess(),
                gameRules
        );
        cachedSlimeWorldConfig = paperConfigurations.createWorldConfig(contextMap);
    }


}
