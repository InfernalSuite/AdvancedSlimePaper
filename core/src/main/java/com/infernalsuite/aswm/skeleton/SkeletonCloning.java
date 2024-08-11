package com.infernalsuite.aswm.skeleton;

import com.flowpowered.nbt.CompoundTag;
import com.infernalsuite.aswm.Util;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.utils.NibbleArray;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import com.infernalsuite.aswm.api.world.SlimeWorld;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.*;

public class SkeletonCloning {

    public static SkeletonSlimeWorld fullClone(String worldName, SlimeWorld world, SlimeLoader loader) {
        return new SkeletonSlimeWorld(worldName,
                loader == null ? world.getLoader() : loader,
                loader == null || world.isReadOnly(),
                cloneChunkStorage(world.getChunkStorage()),
                world.getExtraData().clone(),
                world.getPropertyMap().clone(),
                world.getDataVersion());
    }

    public static SkeletonSlimeWorld weakCopy(SlimeWorld world) {
        Long2ObjectMap<SlimeChunk> cloned = new Long2ObjectOpenHashMap<>();
        for (SlimeChunk chunk : world.getChunkStorage()) {
            long pos = Util.chunkPosition(chunk.getX(), chunk.getZ());

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


    private static Long2ObjectMap<SlimeChunk> cloneChunkStorage(Collection<SlimeChunk> slimeChunkMap) {
        Long2ObjectMap<SlimeChunk> cloned = new Long2ObjectOpenHashMap<>();
        for (SlimeChunk chunk : slimeChunkMap) {
            long pos = Util.chunkPosition(chunk.getX(), chunk.getZ());

            SlimeChunkSection[] copied = new SlimeChunkSection[chunk.getSections().length];
            for (int i = 0; i < copied.length; i++) {
                SlimeChunkSection original = chunk.getSections()[i];
                if (original == null) continue; // This shouldn't happen, yet it does, not gonna figure out why.

                NibbleArray blockLight = original.getBlockLight();
                NibbleArray skyLight = original.getSkyLight();

                copied[i] = new SlimeChunkSectionSkeleton(
                        original.getBlockStatesTag() == null ? null : original.getBlockStatesTag().clone(),
                        original.getBiomeTag() == null ? null : original.getBiomeTag().clone(),
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
                            chunk.getExtraData().clone(),
                            null
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
