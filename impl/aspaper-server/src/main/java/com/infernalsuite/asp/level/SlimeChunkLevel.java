package com.infernalsuite.asp.level;

import ca.spottedleaf.moonrise.common.list.EntityList;
import ca.spottedleaf.moonrise.patches.chunk_system.level.ChunkSystemServerLevel;
import ca.spottedleaf.moonrise.patches.chunk_system.level.entity.ChunkEntitySlices;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import org.jetbrains.annotations.Nullable;

public class SlimeChunkLevel extends LevelChunk {

    private final SlimeInMemoryWorld inMemoryWorld;

    public SlimeChunkLevel(SlimeLevelInstance world, ChunkPos pos, UpgradeData upgradeData, LevelChunkTicks<Block> blockTickScheduler, LevelChunkTicks<Fluid> fluidTickScheduler, long inhabitedTime, @Nullable LevelChunkSection[] sectionArrayInitializer, @Nullable LevelChunk.PostLoadProcessor entityLoader, @Nullable BlendingData blendingData) {
        super(world, pos, upgradeData, blockTickScheduler, fluidTickScheduler, inhabitedTime, sectionArrayInitializer, entityLoader, blendingData);
        this.inMemoryWorld = world.slimeInstance;
    }

    @Override
    public void loadCallback() {
        super.loadCallback();
        this.inMemoryWorld.ensureChunkMarkedAsLoaded(this);
    }

    @Override
    public void unloadCallback() {
        super.unloadCallback();

        ChunkEntitySlices entities = ((ChunkSystemServerLevel) this.level).moonrise$getChunkTaskScheduler()
                .chunkHolderManager.getChunkHolder(this.locX, this.locZ).getEntityChunk();
        inMemoryWorld.unload(this, entities);
    }
}
