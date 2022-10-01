package com.grinderwolf.swm.nms.v1191;

import com.grinderwolf.swm.clsm.CLSMBridge;
import com.grinderwolf.swm.clsm.ClassModifier;
import com.mojang.datafixers.util.Either;
import lombok.AccessLevel;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.storage.EntityStorage;
import net.minecraft.world.level.entity.ChunkEntities;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CraftCLSMBridge implements CLSMBridge {

    private final v1191SlimeNMS nmsInstance;

    @Override
    public Object getChunk(Object worldObject, int x, int z) {
        CustomWorldServer world = (CustomWorldServer) worldObject;
        return Either.left(world.getImposterChunk(x, z));
    }

    @Override
    public boolean saveChunk(Object world, Object chunkAccess) {
        if (!(world instanceof CustomWorldServer)) {
            return false; // Returning false will just run the original saveChunk method
        }

        if (!(chunkAccess instanceof ImposterProtoChunk || chunkAccess instanceof LevelChunk) || !((ChunkAccess) chunkAccess).isUnsaved()) {
            // We're only storing fully-loaded chunks that need to be saved
            return true;
        }

        LevelChunk chunk;

        if (chunkAccess instanceof ImposterProtoChunk) {
            chunk = ((ImposterProtoChunk) chunkAccess).getWrapped();
        } else {
            chunk = (LevelChunk) chunkAccess;
        }

        ((CustomWorldServer) world).saveChunk(chunk);
        chunk.setUnsaved(false);

        return true;
    }

    @Override
    public Object loadEntities(Object storage, Object chunkCoords) {
        EntityStorage entityStorage = (EntityStorage) storage;
        if (!isCustomWorld(entityStorage.level)) {
            return null;
        }


        return ((CustomWorldServer) entityStorage.level).handleEntityLoad(entityStorage, (ChunkPos) chunkCoords);
    }

    @Override
    public boolean storeEntities(Object storage, Object entityList) {
        EntityStorage entityStorage = (EntityStorage) storage;
        if (!isCustomWorld(entityStorage.level)) {
            return false;
        }

        ((CustomWorldServer) entityStorage.level).handleEntityUnLoad(entityStorage, (ChunkEntities<Entity>) entityList);
        return true;
    }

    @Override
    public boolean flushEntities(Object storage) {
        EntityStorage entityStorage = (EntityStorage) storage;
        return isCustomWorld(entityStorage.level);
    }

    @Override
    public boolean isCustomWorld(Object world) {
        if (world instanceof CustomWorldServer) {
            return true;
        } else if (world instanceof Level) {
            return false;
        } else {
            throw new IllegalStateException("World is probably not a world, was given %s. Check the classmodifier to ensure the correct level field is passed (check for field name changes)".formatted(world));
        }
    }

    @Override
    public Object injectCustomWorlds() {
        return nmsInstance.injectDefaultWorlds();
    }

    static void initialize(v1191SlimeNMS instance) {
        ClassModifier.setLoader(new CraftCLSMBridge(instance));
    }

    @Override
    public @NonNull Object getChunkTask(Object scheduler, Object world, int chunkX, int chunkZ, Object chunkHolder, Object priority, Object status) {
        throw new UnsupportedOperationException();
    }
}
