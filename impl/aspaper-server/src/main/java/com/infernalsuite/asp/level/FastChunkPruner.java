package com.infernalsuite.asp.level;

import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

public class FastChunkPruner {

    public static boolean canBePruned(SlimeWorld world, LevelChunk chunk) {
        // Kenox <muranelp@gmail.com>
        // It's not safe to assume that the chunk can be pruned
        // if there isn't a loaded chunk there
        if (chunk == null || chunk.getChunkHolder() == null) {
            return false;
        }

        SlimePropertyMap propertyMap = world.getPropertyMap();
        if (propertyMap.getValue(SlimeProperties.SHOULD_LIMIT_SAVE)) {
            int minX = propertyMap.getValue(SlimeProperties.SAVE_MIN_X);
            int maxX = propertyMap.getValue(SlimeProperties.SAVE_MAX_X);

            int minZ = propertyMap.getValue(SlimeProperties.SAVE_MIN_Z);
            int maxZ = propertyMap.getValue(SlimeProperties.SAVE_MAX_Z);

            int chunkX = chunk.locX;
            int chunkZ = chunk.locZ;

            if (chunkX < minX || chunkX > maxX) {
                return true;
            }

            if (chunkZ < minZ || chunkZ > maxZ) {
                return true;
            }
        }

        String pruningSetting = world.getPropertyMap().getValue(SlimeProperties.CHUNK_PRUNING);
        if (pruningSetting.equals("aggressive")) {
            ChunkEntitySlices slices = chunk.getChunkHolder().getEntityChunk();

            return chunk.blockEntities.isEmpty() && (slices == null || slices.isEmpty()) && areSectionsEmpty(chunk);
        }

        return false;
    }

    private static boolean areSectionsEmpty(LevelChunk chunk) {
        for (LevelChunkSection section : chunk.getSections()) {
            if (!section.hasOnlyAir()) {
                return false;
            }
        }

        return true;
    }
}
