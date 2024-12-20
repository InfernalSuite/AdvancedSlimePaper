package com.infernalsuite.asp.skeleton;

import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.Nullable;

public record SlimeChunkSectionSkeleton(CompoundBinaryTag blockStates, CompoundBinaryTag biome, NibbleArray block, NibbleArray light) implements SlimeChunkSection {
    @Override
    public CompoundBinaryTag getBlockStatesTag() {
        return this.blockStates;
    }

    @Override
    public CompoundBinaryTag getBiomeTag() {
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
