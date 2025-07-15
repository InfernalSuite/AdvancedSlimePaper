package com.infernalsuite.asp.level.chunk;

import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.level.SlimeInMemoryWorld;
import com.infernalsuite.asp.level.SlimeLevelInstance;
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
    private final NMSSlimeChunk nmsSlimeChunk;
    private final @Nullable SlimeChunk slimeReference;

    public SlimeChunkLevel(
            SlimeLevelInstance world,
            @Nullable SlimeChunk reference,
            ChunkPos pos,
            UpgradeData upgradeData,
            LevelChunkTicks<Block> blockTickScheduler,
            LevelChunkTicks<Fluid> fluidTickScheduler,
            long inhabitedTime,
            @Nullable LevelChunkSection[] sectionArrayInitializer,
            @Nullable LevelChunk.PostLoadProcessor entityLoader,
            @Nullable BlendingData blendingData
    ) {
        super(world, pos, upgradeData, blockTickScheduler, fluidTickScheduler, inhabitedTime, sectionArrayInitializer, entityLoader, blendingData);
        this.inMemoryWorld = world.slimeInstance;
        this.nmsSlimeChunk = new NMSSlimeChunk(this, reference);
        this.slimeReference = reference;
    }

    @Override
    public void loadCallback() {
        //Not the earliest point where we can do promote the chunk in storage, but it's the easiest without any further patches,
        //and without causing a potential memory leak, and It's where bukkit calls its chunk load event so we should be fine.
        this.inMemoryWorld.promoteInChunkStorage(this);

        super.loadCallback();
    }

    public SlimeChunk getSafeSlimeReference() {
        if(this.slimeReference == null) return this.nmsSlimeChunk;
        return new SafeNmsChunkWrapper(this.nmsSlimeChunk, this.slimeReference);
    }

    public NMSSlimeChunk getNmsSlimeChunk() {
        return nmsSlimeChunk;
    }
}
