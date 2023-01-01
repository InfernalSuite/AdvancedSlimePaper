package com.infernalsuite.aswm.skeleton;

import com.flowpowered.nbt.CompoundTag;
import com.infernalsuite.aswm.utils.NibbleArray;
import com.infernalsuite.aswm.world.SlimeChunkSection;
import org.jetbrains.annotations.Nullable;

public record SlimeChunkSectionSkeleton(CompoundTag blockStates, CompoundTag biome, NibbleArray block, NibbleArray light) implements SlimeChunkSection {
    @Override
    public CompoundTag getBlockStatesTag() {
        return this.blockStates;
    }

    @Override
    public CompoundTag getBiomeTag() {
        return this.biome;
    }

    @Override
    public @Nullable NibbleArray getBlockLight() {
        return this.block;
    }

    @Override
    public NibbleArray getSkyLight() {
        return this.light;
    }
}
