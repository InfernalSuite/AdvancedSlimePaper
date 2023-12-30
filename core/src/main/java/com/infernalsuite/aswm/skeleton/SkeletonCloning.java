package com.infernalsuite.aswm.skeleton;

import com.flowpowered.nbt.CompoundTag;
import com.infernalsuite.aswm.ChunkPos;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.utils.NibbleArray;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import com.infernalsuite.aswm.api.world.SlimeWorld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SkeletonCloning {

    public static SkeletonSlimeWorld fullClone(String worldName, SlimeWorld world, SlimeLoader loader) {
        return new SkeletonSlimeWorld(worldName,
                loader == null ? world.getLoader() : loader,
                world.isReadOnly(),
                cloneChunkStorage(world.getChunkStorage()),
                world.getExtraData().clone(),
                world.getPropertyMap().clone(),
                world.getDataVersion());
    }

    public static SkeletonSlimeWorld weakCopy(SlimeWorld world) {
        Map<ChunkPos, SlimeChunk> cloned = new HashMap<>();
        for (SlimeChunk chunk : world.getChunkStorage()) {
            ChunkPos pos = new ChunkPos(chunk.getX(), chunk.getZ());

            cloned.put(pos, chunk);
        }

        return new SkeletonSlimeWorld(world.getName(),
                world.getLoader(),
                world.isReadOnly(),
                cloned,
                world.getExtraData().clone(),
                world.getPropertyMap().clone(),
                world.getDataVersion());
    }


    private static Map<ChunkPos, SlimeChunk> cloneChunkStorage(Collection<SlimeChunk> slimeChunkMap) {
        Map<ChunkPos, SlimeChunk> cloned = new HashMap<>();
        for (SlimeChunk chunk : slimeChunkMap) {
            ChunkPos pos = new ChunkPos(chunk.getX(), chunk.getZ());

            SlimeChunkSection[] copied = new SlimeChunkSection[chunk.getSections().length];
            for (int i = 0; i < copied.length; i++) {
                SlimeChunkSection original = chunk.getSections()[i];

                NibbleArray blockLight = original.getBlockLight();
                NibbleArray skyLight = original.getSkyLight();

                copied[i] = new SlimeChunkSectionSkeleton(
                        original.getBlockStatesTag().clone(),
                        original.getBiomeTag().clone(),
                        blockLight == null ? null : blockLight.clone(),
                        skyLight == null ? null : skyLight.clone()
                );
            }

            cloned.put(pos,
                    new SlimeChunkSkeleton(
                            chunk.getX(),
                            chunk.getZ(),
                            copied,
                            chunk.getHeightMaps().clone(),
                            deepClone(chunk.getTileEntities()),
                            deepClone(chunk.getEntities()),
                            chunk.getExtraData().clone()
                    ));
        }

        return cloned;
    }

    private static List<CompoundTag> deepClone(List<CompoundTag> tags) {
        List<CompoundTag> cloned = new ArrayList<>(tags.size());
        for (CompoundTag tag : tags) {
            cloned.add(tag.clone());
        }

        return cloned;
    }
}
