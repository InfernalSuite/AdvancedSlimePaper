package com.infernalsuite.asp.api;

import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.util.Services;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;

@ApiStatus.Internal
public interface SlimeNMSBridge {

    // Overriding
    boolean loadOverworldOverride();

    boolean loadNetherOverride();

    boolean loadEndOverride();

    void setDefaultWorlds(SlimeWorld normalWorld, SlimeWorld netherWorld, SlimeWorld endWorld) throws IOException;

    SlimeWorldInstance loadInstance(SlimeWorld slimeWorld);

    SlimeWorldInstance getInstance(World world);

    // Will return new (fixed) instance
    SlimeWorld applyDataFixers(SlimeWorld world);

    int getCurrentVersion();

    static SlimeNMSBridge instance() {
        return Holder.INSTANCE;
    }

    void extractCraftPDC(PersistentDataContainer source, CompoundBinaryTag.Builder builder);

    @ApiStatus.Internal
    class Holder {
        private static final SlimeNMSBridge INSTANCE = Services.service(SlimeNMSBridge.class).orElseThrow();
    }

}
