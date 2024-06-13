package com.infernalsuite.aswm.api.world;

import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

/**
 * A "running" slime world, bound to a loaded ("live") Minecraft world.
 */
public interface ActiveSlimeWorld extends SlimeWorld {
    /**
     * Returns the bukkit world for this loaded slime world.
     *
     * @return Bukkit world
     */
    @NotNull
    World getBukkitWorld();

    /**
     * Returns a snapshot of the world.
     *
     * @return Snapshot of the world, ready for serialization.
     */
    @NotNull
    SlimeWorld getSnapshot();
}
