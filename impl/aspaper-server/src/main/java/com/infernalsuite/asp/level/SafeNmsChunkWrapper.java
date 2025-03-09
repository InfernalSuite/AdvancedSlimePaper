package com.infernalsuite.asp.level;

import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.List;

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
