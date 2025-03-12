package com.infernalsuite.asp.api.world;

import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import net.kyori.adventure.nbt.BinaryTag;
import org.bukkit.World;

import java.util.concurrent.ConcurrentMap;

public interface SlimeWorldInstance {

    String getName();

    World getBukkitWorld();

    SlimeWorld getSlimeWorld();

    SlimePropertyMap getPropertyMap();

    boolean isReadOnly();

    SlimeLoader getLoader();

    ConcurrentMap<String, BinaryTag> getExtraData();

}
