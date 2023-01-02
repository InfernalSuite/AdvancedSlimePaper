package com.infernalsuite.aswm.skeleton;

import com.flowpowered.nbt.CompoundTag;
import com.infernalsuite.aswm.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.aswm.loaders.SlimeLoader;
import com.infernalsuite.aswm.utils.NibbleArray;
import com.infernalsuite.aswm.world.SlimeChunk;
import com.infernalsuite.aswm.world.SlimeChunkSection;
import com.infernalsuite.aswm.world.SlimeWorld;
import com.infernalsuite.aswm.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record SkeletonSlimeWorld(
        String name,
        @Nullable SlimeLoader loader,
        Map<ChunkPos, SlimeChunk> chunkStorage,
        CompoundTag extraSerialized,
        SlimePropertyMap slimePropertyMap,
        int dataVersion
) implements SlimeWorld {
    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public SlimeLoader getLoader() {
        return this.loader;
    }

    @Override
    public SlimeChunk getChunk(int x, int z) {
        return this.chunkStorage.get(new ChunkPos(x, z));
    }

    @Override
    public Collection<SlimeChunk> getChunkStorage() {
        return this.chunkStorage.values();
    }

    @Override
    public CompoundTag getExtraData() {
        return this.extraSerialized;
    }

    @Override
    public Collection<CompoundTag> getWorldMaps() {
        return List.of();
    }

    @Override
    public SlimePropertyMap getPropertyMap() {
        return this.slimePropertyMap;
    }

    @Override
    public boolean isReadOnly() {
        return this.loader == null;
    }

    @Override
    public int getDataVersion() {
        return this.dataVersion;
    }

    @Override
    public SlimeWorld clone(String worldName) {
        try {
            return clone(worldName, null);
        } catch (WorldAlreadyExistsException | IOException ignored) {
            return null; // Never going to happen
        }
    }

    @Override
    public SlimeWorld clone(String worldName, SlimeLoader loader) throws WorldAlreadyExistsException, IOException {
        if (name.equals(worldName)) {
            throw new IllegalArgumentException("The clone world cannot have the same name as the original world!");
        }

        if (worldName == null) {
            throw new IllegalArgumentException("The world name cannot be null!");
        }

        if (loader != null) {
            if (loader.worldExists(worldName)) {
                throw new WorldAlreadyExistsException(worldName);
            }
        }

        Map<ChunkPos, SlimeChunk> cloned = new HashMap<>();
        for (Map.Entry<ChunkPos, SlimeChunk> entry : this.chunkStorage.entrySet()) {
            SlimeChunk value = entry.getValue();

            SlimeChunkSection[] copied = new SlimeChunkSection[value.getSections().length];
            for (int i = 0; i < copied.length; i++) {
                SlimeChunkSection original = value.getSections()[i];

                NibbleArray blockLight = original.getBlockLight();
                NibbleArray skyLight = original.getSkyLight();

                copied[i] = new SlimeChunkSectionSkeleton(
                        original.getBlockStatesTag().clone(),
                        original.getBiomeTag().clone(),
                        blockLight == null ? null : blockLight.clone(),
                        skyLight == null ? null : skyLight.clone()
                );
            }

            cloned.put(entry.getKey(),
                    new SlimeChunkSkeleton(
                            value.getX(),
                            value.getZ(),
                            copied,
                            value.getHeightMaps().clone(),
                            this.deepClone(value.getTileEntities()),
                            this.deepClone(value.getEntities())
                    ));
        }

        return new SkeletonSlimeWorld(
                worldName,
                loader,
                cloned,
                this.extraSerialized.clone(),
                this.slimePropertyMap.clone(),
                this.dataVersion
        );
    }

    private List<CompoundTag> deepClone(List<CompoundTag> tags) {
        List<CompoundTag> cloned = new ArrayList<>(tags.size());
        for (CompoundTag tag : tags) {
            cloned.add(tag.clone());
        }

        return cloned;
    }

}
