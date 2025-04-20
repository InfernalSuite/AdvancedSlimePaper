package com.infernalsuite.asp.skeleton;

import com.infernalsuite.asp.Util;
import com.infernalsuite.asp.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.pdc.AdventurePersistentDataContainer;
import com.infernalsuite.asp.serialization.slime.SlimeSerializer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;

public final class SkeletonSlimeWorld implements SlimeWorld {
    private final String name;
    private final @Nullable SlimeLoader loader;
    private final boolean readOnly;
    private final Long2ObjectMap<SlimeChunk> chunkStorage;
    private final ConcurrentMap<String, BinaryTag> extraSerialized;
    private final SlimePropertyMap slimePropertyMap;
    private final int dataVersion;
    private final AdventurePersistentDataContainer pdc;

    public SkeletonSlimeWorld(
            String name,
            @Nullable SlimeLoader loader,
            boolean readOnly,
            Long2ObjectMap<SlimeChunk> chunkStorage,
            ConcurrentMap<String, BinaryTag> extraSerialized,
            SlimePropertyMap slimePropertyMap,
            int dataVersion
    ) {
        this.name = name;
        this.loader = loader;
        this.readOnly = readOnly;
        this.chunkStorage = chunkStorage;
        this.extraSerialized = extraSerialized;
        this.slimePropertyMap = slimePropertyMap;
        this.dataVersion = dataVersion;
        this.pdc = new AdventurePersistentDataContainer(extraSerialized);
    }

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
        return this.chunkStorage.get(Util.chunkPosition(x, z));
    }

    @Override
    public Collection<SlimeChunk> getChunkStorage() {
        return this.chunkStorage.values();
    }

    @Override
    public ConcurrentMap<String, BinaryTag> getExtraData() {
        return this.extraSerialized;
    }

    @Override
    public Collection<CompoundBinaryTag> getWorldMaps() {
        return List.of();
    }

    @Override
    public SlimePropertyMap getPropertyMap() {
        return this.slimePropertyMap;
    }

    @Override
    public boolean isReadOnly() {
        return this.readOnly || this.loader == null;
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

        //Make new worlds always non-read-only. if the provided loader is null, the fullClone method will set it to true again
        SlimeWorld cloned = SkeletonCloning.fullClone(worldName, this, loader, false);
        if (loader != null) {
            loader.saveWorld(worldName, SlimeSerializer.serialize(cloned));
        }

        return cloned;
    }

    @Override
    public @NotNull PersistentDataContainer getPersistentDataContainer() {
        return this.pdc;
    };

    public String name() {
        return name;
    }

    public @Nullable SlimeLoader loader() {
        return loader;
    }

    public boolean readOnly() {
        return readOnly;
    }

    public Long2ObjectMap<SlimeChunk> chunkStorage() {
        return chunkStorage;
    }

    public ConcurrentMap<String, BinaryTag> extraSerialized() {
        return extraSerialized;
    }

    public SlimePropertyMap slimePropertyMap() {
        return slimePropertyMap;
    }

    public int dataVersion() {
        return dataVersion;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SkeletonSlimeWorld) obj;
        return Objects.equals(this.name, that.name) &&
               Objects.equals(this.loader, that.loader) &&
               this.readOnly == that.readOnly &&
               Objects.equals(this.chunkStorage, that.chunkStorage) &&
               Objects.equals(this.extraSerialized, that.extraSerialized) &&
               Objects.equals(this.slimePropertyMap, that.slimePropertyMap) &&
               this.dataVersion == that.dataVersion;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, loader, readOnly, chunkStorage, extraSerialized, slimePropertyMap, dataVersion);
    }

    @Override
    public String toString() {
        return "SkeletonSlimeWorld[" +
               "name=" + name + ", " +
               "loader=" + loader + ", " +
               "readOnly=" + readOnly + ", " +
               "chunkStorage=" + chunkStorage + ", " +
               "extraSerialized=" + extraSerialized + ", " +
               "slimePropertyMap=" + slimePropertyMap + ", " +
               "dataVersion=" + dataVersion + ']';
    }

}
