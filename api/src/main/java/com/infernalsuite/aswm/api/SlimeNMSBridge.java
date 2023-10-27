package com.infernalsuite.aswm.api;

import com.flowpowered.nbt.CompoundMap;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.SlimeWorldInstance;
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

    void extractCraftPDC(PersistentDataContainer source, CompoundMap target);

    PersistentDataContainer extractCompoundMapIntoCraftPDC(CompoundMap source);

    @ApiStatus.Internal
    static class Holder {

        private static final SlimeNMSBridge INSTANCE = Services.service(SlimeNMSBridge.class).orElseThrow();

    }

}
