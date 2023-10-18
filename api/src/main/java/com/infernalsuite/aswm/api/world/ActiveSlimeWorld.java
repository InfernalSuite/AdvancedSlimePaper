package com.infernalsuite.aswm.api.world;

/**
 * A "running" slime world, from which immutable snapshots can be taken whenever wanted.
 */
public interface ActiveSlimeWorld {
    /**
     * Returns a snapshot of the world.
     *
     * @return Snapshot of the world, ready for serialization.
     */
    SlimeWorld getSnapshot();
}
