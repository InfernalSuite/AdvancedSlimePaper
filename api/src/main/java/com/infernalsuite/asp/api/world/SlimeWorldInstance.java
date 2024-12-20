package com.infernalsuite.asp.api.world;

import com.infernalsuite.asp.api.loaders.SlimeLoader;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.World;

public interface SlimeWorldInstance {

    String getName();

    World getBukkitWorld();

    SlimeWorld getSlimeWorldMirror();

    com.infernalsuite.asp.api.world.properties.SlimePropertyMap getPropertyMap();

    boolean isReadOnly();

    SlimeLoader getLoader();

    CompoundBinaryTag getExtraData();

}
