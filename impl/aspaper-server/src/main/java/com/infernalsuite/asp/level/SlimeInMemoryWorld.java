package com.infernalsuite.asp.level;

import com.infernalsuite.asp.ChunkPos;
import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.pdc.AdventurePersistentDataContainer;
import com.infernalsuite.asp.serialization.slime.SlimeSerializer;
import com.infernalsuite.asp.skeleton.SkeletonCloning;
import com.infernalsuite.asp.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.asp.skeleton.SlimeChunkSkeleton;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.SlimeWorldInstance;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/*
The concept of this is a bit flawed, since ideally this should be a 1:1 representation of the MC world.
However, due to the complexity of the chunk system we essentially need to wrap around it.
This stores slime chunks, and when unloaded, will properly convert it to a normal slime chunk for storage.
 */
public class SlimeInMemoryWorld implements SlimeWorld, SlimeWorldInstance {

    private final SlimeLevelInstance instance;
    private final SlimeWorld liveWorld;

    private final ConcurrentMap<String, BinaryTag> extra;
    private final AdventurePersistentDataContainer extraPDC;
    private final SlimePropertyMap propertyMap;
    private final SlimeLoader loader;

    private final Map<ChunkPos, SlimeChunk> chunkStorage = new HashMap<>();
    private boolean readOnly;
    // private final Map<ChunkPos, List<CompoundTag>> entityStorage = new HashMap<>();

    public SlimeInMemoryWorld(SlimeBootstrap bootstrap, SlimeLevelInstance instance) {
        this.instance = instance;
        this.extra = bootstrap.initial().getExtraData();
        this.propertyMap = bootstrap.initial().getPropertyMap();
        this.loader = bootstrap.initial().getLoader();
        this.readOnly = bootstrap.initial().isReadOnly();

        for (SlimeChunk initial : bootstrap.initial().getChunkStorage()) {
            ChunkPos pos = new ChunkPos(initial.getX(), initial.getZ());
            List<CompoundBinaryTag> tags = new ArrayList<>(initial.getEntities());

            //  this.entityStorage.put(pos, tags);
            this.chunkStorage.put(pos, initial);
        }

        this.extraPDC = new AdventurePersistentDataContainer(this.extra);
        this.liveWorld = new NMSSlimeWorld(this);
    }

    @Override
    public String getName() {
        return this.instance.getMinecraftWorld().serverLevelData.getLevelName();
    }

    @Override
    public SlimeLoader getLoader() {
        return this.loader;
    }

    public LevelChunk promote(int x, int z, SlimeChunk chunk) {
        SlimeChunkLevel levelChunk;
        if (chunk == null) {
            net.minecraft.world.level.ChunkPos pos = new net.minecraft.world.level.ChunkPos(x, z);
            LevelChunkTicks<Block> blockLevelChunkTicks = new LevelChunkTicks<>();
            LevelChunkTicks<Fluid> fluidLevelChunkTicks = new LevelChunkTicks<>();

            levelChunk = new SlimeChunkLevel(this.instance, pos, UpgradeData.EMPTY, blockLevelChunkTicks, fluidLevelChunkTicks,
                    0L, null, null, null);

            chunk = new NMSSlimeChunk(levelChunk, getChunk(x, z));

        } else {
            levelChunk = SlimeChunkConverter.deserializeSlimeChunk(this.instance, chunk);
            chunk = new SafeNmsChunkWrapper(new NMSSlimeChunk(levelChunk, chunk), chunk);
        }
        this.chunkStorage.put(new ChunkPos(x, z), chunk);

        return levelChunk;
    }

    // Authored by: Kenox <muranelp@gmail.com>
    // Don't use the NMS live chunk in the chunk map
    public void unload(LevelChunk providedChunk) {
        final int x = providedChunk.locX;
        final int z = providedChunk.locZ;

        SlimeChunk chunk = new NMSSlimeChunk(providedChunk, getChunk(x, z));

        if (FastChunkPruner.canBePruned(this.liveWorld, providedChunk)) {
            this.chunkStorage.remove(new ChunkPos(x, z));
            return;
        }

        this.chunkStorage.put(new ChunkPos(x, z),
                new SlimeChunkSkeleton(chunk.getX(), chunk.getZ(), chunk.getSections(),
                        chunk.getHeightMaps(), chunk.getTileEntities(), chunk.getEntities(), chunk.getExtraData()));
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
    public World getBukkitWorld() {
        return this.instance.getWorld();
    }

    @Override
    public SlimeWorld getSlimeWorldMirror() {
        return this.liveWorld;
    }

    @Override
    public SlimePropertyMap getPropertyMap() {
        return this.propertyMap;
    }

    @Override
    public boolean isReadOnly() {
        return this.getSaveStrategy() == null || this.readOnly;
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
        if (this.getName().equals(worldName)) {
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

        SlimeWorld cloned = SkeletonCloning.fullClone(worldName, this, loader);
        if (loader != null) {
            loader.saveWorld(worldName, SlimeSerializer.serialize(cloned));
        }

        return cloned;
    }

    @Override
    public int getDataVersion() {
        return this.liveWorld.getDataVersion();
    }

    @Override
    public SlimeLoader getSaveStrategy() {
        return this.loader;
    }

    @Override
    public ConcurrentMap<String, BinaryTag> getExtraData() {
        return this.extra;
    }

    @Override
    public Collection<CompoundBinaryTag> getWorldMaps() {
        return List.of();
    }

    //    public Map<ChunkPos, List<CompoundTag>> getEntityStorage() {
    //        return entityStorage;
    //    }

    public SlimeWorld getForSerialization() {
        SlimeWorld world = SkeletonCloning.weakCopy(this);

        Map<ChunkPos, SlimeChunk> cloned = new HashMap<>();
        for (Map.Entry<ChunkPos, SlimeChunk> entry : this.chunkStorage.entrySet()) {
            SlimeChunk clonedChunk = entry.getValue();
            // NMS "live" chunks need to be converted
            {
                LevelChunk chunk = null;
                if (clonedChunk instanceof SafeNmsChunkWrapper safeNmsChunkWrapper) {
                    if (safeNmsChunkWrapper.shouldDefaultBackToSlimeChunk()) {
                        clonedChunk = safeNmsChunkWrapper.getSafety();
                    } else {
                        chunk = safeNmsChunkWrapper.getWrapper().getChunk();
                    }
                } else if  (clonedChunk instanceof NMSSlimeChunk nmsSlimeChunk) {
                    chunk = nmsSlimeChunk.getChunk();
                }

                if (chunk != null) {
                    if (FastChunkPruner.canBePruned(world, chunk)) {
                        continue;
                    }

                    // Serialize Bukkit Values (PDC)

                    CompoundBinaryTag adventureTag = Converter.convertTag(chunk.persistentDataContainer.toTagCompound());
                    clonedChunk.getExtraData().put("ChunkBukkitValues", adventureTag);

                    clonedChunk = new SlimeChunkSkeleton(
                            clonedChunk.getX(),
                            clonedChunk.getZ(),
                            clonedChunk.getSections(),
                            clonedChunk.getHeightMaps(),
                            clonedChunk.getTileEntities(),
                            clonedChunk.getEntities(),
                            clonedChunk.getExtraData()
                    );
                }
            }

            cloned.put(entry.getKey(), clonedChunk);
        }

        // Serialize Bukkit Values (PDC)

        var nmsTag = new net.minecraft.nbt.CompoundTag();
        this.instance.getWorld().storeBukkitValues(nmsTag);

        // Bukkit stores the relevant tag as a tag with the key "BukkitValues" in the tag we supply to it
        var adventureTag = Converter.convertTag(nmsTag.getCompound("BukkitValues"));

        world.getExtraData().put("BukkitValues", adventureTag);

        return new SkeletonSlimeWorld(world.getName(),
                world.getLoader(),
                world.isReadOnly(),
                cloned,
                world.getExtraData(),
                world.getPropertyMap(),
                world.getDataVersion()
        );
    }

    public SlimeLevelInstance getInstance() {
        return instance;
    }

    @Override
    public @NotNull PersistentDataContainer getPersistentDataContainer() {
        return this.extraPDC;
    }
}
