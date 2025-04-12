package com.infernalsuite.asp.api;

import com.infernalsuite.asp.api.world.SlimeWorld;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.ApiStatus;

import java.util.List;

@ApiStatus.Internal
public interface SlimeDataConverter {

    // Will return new (fixed) instance
    SlimeWorld applyDataFixers(SlimeWorld world);

    CompoundBinaryTag convertChunkTo1_13(CompoundBinaryTag globalTag);

    List<CompoundBinaryTag> convertEntities(List<CompoundBinaryTag> input, int from, int to);
    List<CompoundBinaryTag> convertTileEntities(List<CompoundBinaryTag> input, int from, int to);
    ListBinaryTag convertBlockPalette(ListBinaryTag input, int from, int to);

}
