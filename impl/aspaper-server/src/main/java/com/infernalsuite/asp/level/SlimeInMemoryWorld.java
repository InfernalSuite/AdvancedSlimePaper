package com.infernalsuite.asp.level;

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.Util;
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
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.bukkit.World;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/*
The concept of this is a bit flawed, since ideally this should be a 1:1 representation of the MC world.
However, due to the complexity of the chunk system we essentially need to wrap around it.
This stores slime chunks, and when unloaded, will properly convert it to a normal slime chunk for storage.
 */
public class SlimeInMemoryWorld implements SlimeWorld, SlimeWorldInstance {

    private final SlimeLevelInstance instance;

    private final ConcurrentMap<String, BinaryTag> extra;
    private final AdventurePersistentDataContainer extraPDC;
    private final SlimePropertyMap propertyMap;
    private final SlimeLoader loader;

    private final Long2ObjectMap<SlimeChunk> chunkStorage = new Long2ObjectOpenHashMap<>();
    private boolean readOnly;

    public SlimeInMemoryWorld(SlimeBootstrap bootstrap, SlimeLevelInstance instance) {
        this.instance = instance;
        this.extra = bootstrap.initial().getExtraData();
        this.propertyMap = bootstrap.initial().getPropertyMap();
        this.loader = bootstrap.initial().getLoader();
        this.readOnly = bootstrap.initial().isReadOnly();

        for (SlimeChunk initial : bootstrap.initial().getChunkStorage()) {
            long pos = Util.chunkPosition(initial.getX(), initial.getZ());

            this.chunkStorage.put(pos, initial);
        }

        this.extraPDC = new AdventurePersistentDataContainer(this.extra);
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

            //Make SlimeProperties.DEFAULT_BIOME work
            levelChunk.fillBiomesFromNoise(instance.chunkSource.getGenerator().getBiomeSource(),
                    instance.chunkSource.randomState().sampler());

            chunk = new NMSSlimeChunk(levelChunk, getChunk(x, z));
        } else {
            levelChunk = SlimeChunkConverter.deserializeSlimeChunk(this.instance, chunk);
            chunk = new SafeNmsChunkWrapper(new NMSSlimeChunk(levelChunk, chunk), chunk);
        }
        this.chunkStorage.put(Util.chunkPosition(x, z), chunk);

        return levelChunk;
    }

    // Authored by: Kenox <muranelp@gmail.com>
    // Don't use the NMS live chunk in the chunk map
    public void unload(LevelChunk providedChunk) {
        final int x = providedChunk.locX;
        final int z = providedChunk.locZ;

        if (FastChunkPruner.canBePruned(this, providedChunk)) {
            this.chunkStorage.remove(Util.chunkPosition(x, z));
            return;
        }
        SlimeChunk chunk = new NMSSlimeChunk(providedChunk, getChunk(x, z));

        CompoundBinaryTag pdcTag = Converter.convertTag(providedChunk.persistentDataContainer.toTagCompound());
        chunk.getExtraData().put("ChunkBukkitValues", pdcTag);

        this.chunkStorage.put(Util.chunkPosition(x, z),
                new SlimeChunkSkeleton(chunk.getX(), chunk.getZ(), chunk.getSections(),
                        chunk.getHeightMaps(), chunk.getTileEntities(), chunk.getEntities(), chunk.getExtraData(), null));
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
    public @NotNull World getBukkitWorld() {
        return this.instance.getWorld();
    }


    @Override
    public SlimePropertyMap getPropertyMap() {
        return this.propertyMap;
    }

    @Override
    public boolean isReadOnly() {
        return this.getLoader() == null || this.readOnly;
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
        return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    }

    @Override
    public ConcurrentMap<String, BinaryTag> getExtraData() {
        return this.extra;
    }

    @Override
    public Collection<CompoundBinaryTag> getWorldMaps() {
        return List.of();
    }

    public SlimeWorld getForSerialization() {
        SlimeWorld world = SkeletonCloning.weakCopy(this);

        Long2ObjectMap<SlimeChunk> cloned = new Long2ObjectOpenHashMap<>();
        for (Long2ObjectMap.Entry<SlimeChunk> entry : this.chunkStorage.long2ObjectEntrySet()) {
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
                } else if (clonedChunk instanceof NMSSlimeChunk nmsSlimeChunk) {
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
                            clonedChunk.getExtraData(),
                            clonedChunk.getUpgradeData()
                    );
                }
            }

            cloned.put(entry.getLongKey(), clonedChunk);
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

    public void ensureChunkMarkedAsLoaded(SlimeChunkLevel chunk) {
        if (chunkStorage.get(Util.chunkPosition(chunk.locX, chunk.locZ)) instanceof SlimeChunkSkeleton skeleton) {
            chunkStorage.put(Util.chunkPosition(chunk.locX, chunk.locZ), new NMSSlimeChunk(chunk, skeleton));
        }
    }
}
