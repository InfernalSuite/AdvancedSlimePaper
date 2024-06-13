package com.infernalsuite.aswm.api.world;

import com.flowpowered.nbt.CompoundTag;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import org.bukkit.World;

public interface SlimeWorldInstance extends ActiveSlimeWorld {

    String getName();

    World getBukkitWorld();

    ActiveSlimeWorld getSlimeWorldMirror();

    SlimePropertyMap getPropertyMap();

    boolean isReadOnly();

    SlimeLoader getSaveStrategy();

    CompoundTag getExtraData();

}
