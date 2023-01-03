package com.infernalsuite.aswm.world;

import com.flowpowered.nbt.CompoundTag;
import com.infernalsuite.aswm.loaders.SlimeLoader;
import com.infernalsuite.aswm.world.properties.SlimePropertyMap;
import org.bukkit.World;

import java.util.concurrent.CompletableFuture;

public interface SlimeWorldInstance {

    String getName();

    World getBukkitWorld();

    SlimeWorld getSlimeWorldMirror();

    SlimePropertyMap getPropertyMap();

    boolean isReadOnly();

    SlimeLoader getSaveStrategy();

    CompoundTag getExtraData();

}
