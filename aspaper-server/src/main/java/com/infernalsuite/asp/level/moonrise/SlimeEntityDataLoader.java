package com.infernalsuite.asp.level.moonrise;

import ca.spottedleaf.moonrise.patches.chunk_system.io.datacontroller.EntityDataController;
import ca.spottedleaf.moonrise.patches.chunk_system.scheduling.ChunkTaskScheduler;
import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.level.SlimeLevelInstance;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

public class SlimeEntityDataLoader extends EntityDataController {

    private final SlimeLevelInstance instance;

    public SlimeEntityDataLoader(EntityRegionFileStorage storage, ChunkTaskScheduler taskScheduler, SlimeLevelInstance instance) {
        super(storage, taskScheduler);
        this.instance = instance;
    }

    @Override
    public WriteData startWrite(int chunkX, int chunkZ, CompoundTag compound) {
        /*
         * We don't support saving with moonrise as that is way too slow and prevents us from having fast world unloads
         */
        throw new UnsupportedOperationException("Slime should use custom saving method? Is PaperHooks#forceNoSave broken?");
    }

    @Override
    public ReadData readData(int chunkX, int chunkZ) {
        SlimeChunk chunk = instance.getSlimeInstance().getChunk(chunkX, chunkZ);

        if(chunk == null || chunk.getEntities() == null) {
            return new ReadData(ReadData.ReadResult.NO_DATA, null, null, 0);
        }

        CompoundTag tag = new CompoundTag();
        tag.putIntArray("Position", new int[]{chunkX, chunkZ});
        tag.putInt("DataVersion", instance.getSlimeInstance().getDataVersion());

        ListTag listTag = new ListTag();
        for (CompoundBinaryTag entity : chunk.getEntities()) {
            listTag.add(Converter.convertTag(entity));
        }
        tag.put("Entities", listTag);

        return new ReadData(ReadData.ReadResult.SYNC_READ, null, tag, 0);
    }

    @Override
    public CompoundTag finishRead(int chunkX, int chunkZ, ReadData readData) {
        return readData.syncRead();
    }
}
