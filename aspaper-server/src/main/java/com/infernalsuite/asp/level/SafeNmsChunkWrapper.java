package com.infernalsuite.asp.level;

import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class SafeNmsChunkWrapper implements SlimeChunk {

    private final NMSSlimeChunk wrapper;
    private final SlimeChunk safety;

    public SafeNmsChunkWrapper(NMSSlimeChunk wrapper, SlimeChunk safety) {
        this.wrapper = wrapper;
        this.safety = safety;
    }

    @Override
    public int getX() {
        return this.wrapper.getX();
    }

    @Override
    public int getZ() {
        return this.wrapper.getZ();
    }

    @Override
    public SlimeChunkSection[] getSections() {
        if (shouldDefaultBackToSlimeChunk()) {
            return this.safety.getSections();
        }

        return this.wrapper.getSections();
    }

    @Override
    public CompoundBinaryTag getHeightMaps() {
        if (shouldDefaultBackToSlimeChunk()) {
            return this.safety.getHeightMaps();
        }

        return this.wrapper.getHeightMaps();
    }

    @Override
    public List<CompoundBinaryTag> getTileEntities() {
        if (shouldDefaultBackToSlimeChunk()) {
            return this.safety.getTileEntities();
        }

        return this.wrapper.getTileEntities();
    }

    @Override
    public List<CompoundBinaryTag> getEntities() {
        if (shouldDefaultBackToSlimeChunk()) {
            return this.safety.getEntities();
        }

        return this.wrapper.getEntities();
    }

    @Override
    public Map<String, BinaryTag> getExtraData() {
        if (shouldDefaultBackToSlimeChunk()) {
            return this.safety.getExtraData();
        }

        return this.wrapper.getExtraData();
    }

    @Override
    public CompoundBinaryTag getUpgradeData() {
        if (shouldDefaultBackToSlimeChunk()) {
            return this.safety.getUpgradeData();
        }

        return this.wrapper.getUpgradeData();
    }

    @Override
    public @Nullable ListBinaryTag getBlockTicks() {
        if(shouldDefaultBackToSlimeChunk()) {
            return this.safety.getBlockTicks();
        }
        return this.wrapper.getBlockTicks();
    }

    @Override
    public @Nullable ListBinaryTag getFluidTicks() {
        if(shouldDefaultBackToSlimeChunk()) {
            return this.safety.getFluidTicks();
        }
        return this.wrapper.getFluidTicks();
    }

    @Override
    public @Nullable CompoundBinaryTag getPoiChunkSections() {
        if(shouldDefaultBackToSlimeChunk()) {
            return this.safety.getPoiChunkSections();
        }
        return this.wrapper.getPoiChunkSections();
    }

    /*
            Slime chunks can still be requested but not actually loaded, this caused
            some things to not properly save because they are not "loaded" into the chunk.
            See ChunkMap#protoChunkToFullChunk
            anything in the if statement will not be loaded and is stuck inside the runnable.
            Inorder to possibly not corrupt the state, simply refer back to the slime saved object.
            */
    public boolean shouldDefaultBackToSlimeChunk() {
        return this.safety != null && !this.wrapper.getChunk().loaded;
    }

    public NMSSlimeChunk getWrapper() {
        return wrapper;
    }

    public SlimeChunk getSafety() {
        return safety;
    }
}
