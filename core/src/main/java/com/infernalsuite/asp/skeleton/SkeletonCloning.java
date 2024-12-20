package com.infernalsuite.asp.skeleton;

import com.infernalsuite.asp.Util;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.api.world.SlimeWorld;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class SkeletonCloning {

    public static SkeletonSlimeWorld fullClone(String worldName, SlimeWorld world, SlimeLoader loader) {
        return new SkeletonSlimeWorld(worldName,
                loader == null ? world.getLoader() : loader,
                loader == null || world.isReadOnly(),
                cloneChunkStorage(world.getChunkStorage()),
                new ConcurrentHashMap<>(world.getExtraData()),
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
                new ConcurrentHashMap<>(world.getExtraData()),
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
                        original.getBlockStatesTag() == null ? null : CompoundBinaryTag.builder().put(original.getBlockStatesTag()).build(),
                        original.getBiomeTag() == null ? null : CompoundBinaryTag.builder().put(original.getBiomeTag()).build(),
                        blockLight == null ? null : blockLight.clone(),
                        skyLight == null ? null : skyLight.clone()
                );
            }

            cloned.put(pos,
                    new SlimeChunkSkeleton(
                            chunk.getX(),
                            chunk.getZ(),
                            copied,
                            CompoundBinaryTag.builder().put(chunk.getHeightMaps()).build(),
                            deepClone(chunk.getTileEntities()),
                            deepClone(chunk.getEntities()),
                            CompoundBinaryTag.builder().put(chunk.getExtraData()).build(),
                            null
                    ));
        }

        return cloned;
    }

    private static List<CompoundBinaryTag> deepClone(List<CompoundBinaryTag> tags) {
        List<CompoundBinaryTag> cloned = new ArrayList<>(tags.size());
        for (CompoundBinaryTag tag : tags) {
            cloned.add(CompoundBinaryTag.builder().put(tag).build());
        }

        return cloned;
    }
}
