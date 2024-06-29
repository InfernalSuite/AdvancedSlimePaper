package com.infernalsuite.aswm.api.world;

import com.flowpowered.nbt.CompoundTag;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import org.bukkit.World;

public interface SlimeWorldInstance {

    String getName();

    World getBukkitWorld();

    SlimeWorld getSlimeWorldMirror();

    SlimePropertyMap getPropertyMap();

    boolean isReadOnly();

    SlimeLoader getLoader();

    CompoundTag getExtraData();

}
