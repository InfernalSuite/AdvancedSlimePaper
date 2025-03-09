package com.infernalsuite.asp.level;

import com.infernalsuite.asp.api.exceptions.WorldAlreadyExistsException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minecraft.SharedConstants;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.world.level.chunk.LevelChunk;
import org.bukkit.persistence.PersistentDataContainer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class NMSSlimeWorld implements SlimeWorld {

    private final SlimeInMemoryWorld memoryWorld;
    private final SlimeLevelInstance instance;

    public NMSSlimeWorld(SlimeInMemoryWorld memoryWorld) {
        this.instance = memoryWorld.getInstance();
        this.memoryWorld = memoryWorld;
    }

    @Override
    public String getName() {
        return this.instance.getMinecraftWorld().serverLevelData.getLevelName();
    }

    @Override
    public SlimeLoader getLoader() {
        return this.instance.slimeInstance.getLoader();
    }

    @Override
    public SlimeChunk getChunk(int x, int z) {
        LevelChunk chunk = this.instance.getChunkIfLoaded(x, z);
        if (chunk == null) {
            return null;
        }

        return new NMSSlimeChunk(chunk, memoryWorld.getChunk(x, z));
    }

    @Override
    public Collection<SlimeChunk> getChunkStorage() {
        List<ChunkHolder> chunks = ca.spottedleaf.moonrise.common.PlatformHooks.get().getVisibleChunkHolders(this.instance); // Paper
        return chunks.stream().map(ChunkHolder::getFullChunkNow).filter(Objects::nonNull)
                .map((chunkLevel) -> new NMSSlimeChunk(chunkLevel, memoryWorld.getChunk(chunkLevel.getPos().x, chunkLevel.getPos().z))) // This sucks, is there a better way?
                .collect(Collectors.toList());
    }

    @Override
    public ConcurrentMap<String, BinaryTag> getExtraData() {
        return this.instance.slimeInstance.getExtraData();
    }

    @Override
    public Collection<CompoundBinaryTag> getWorldMaps() {
        return List.of();
    }

    @Override
    public SlimePropertyMap getPropertyMap() {
        return this.instance.slimeInstance.getPropertyMap();
    }

    @Override
    public boolean isReadOnly() {
        return this.getLoader() == null;
    }

    @Override
    public SlimeWorld clone(String worldName) {
        return this.memoryWorld.clone(worldName);
    }

    @Override
    public SlimeWorld clone(String worldName, SlimeLoader loader) throws WorldAlreadyExistsException, IOException {
        return this.memoryWorld.clone(worldName, loader);
    }

    @Override
    public int getDataVersion() {
        return SharedConstants.getCurrentVersion().getDataVersion().getVersion();
    }

    @Override
    public @NotNull PersistentDataContainer getPersistentDataContainer() {
        return this.memoryWorld.getPersistentDataContainer();
    }
}
