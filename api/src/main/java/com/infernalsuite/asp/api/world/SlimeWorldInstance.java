package com.infernalsuite.asp.api.world;

import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import net.kyori.adventure.nbt.BinaryTag;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ConcurrentMap;

/*
 * Represents a loaded SlimeWorld. This world is synchronized with the state of the bukkit world.
 */
public interface SlimeWorldInstance extends SlimeWorld {

    @NotNull
    World getBukkitWorld();

}
