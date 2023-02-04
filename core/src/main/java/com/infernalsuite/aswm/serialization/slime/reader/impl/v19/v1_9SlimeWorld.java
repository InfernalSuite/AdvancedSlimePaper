package com.infernalsuite.aswm.serialization.slime.reader.impl.v19;

import com.flowpowered.nbt.CompoundTag;
import com.infernalsuite.aswm.ChunkPos;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;

import java.util.Map;

class v1_9SlimeWorld {

    byte version;
    final String worldName;
    final SlimeLoader loader;
    final Map<ChunkPos, v1_9SlimeChunk> chunks;
    final CompoundTag extraCompound;
    final SlimePropertyMap propertyMap;
    final boolean readOnly;

    v1_9SlimeWorld(byte version,
                   String worldName,
                   SlimeLoader loader,
                   Map<ChunkPos, v1_9SlimeChunk> chunks,
                   CompoundTag extraCompound,
                   SlimePropertyMap propertyMap,
                   boolean readOnly) {
        this.version = version;
        this.worldName = worldName;
        this.loader = loader;
        this.chunks = chunks;
        this.extraCompound = extraCompound;
        this.propertyMap = propertyMap;
        this.readOnly = readOnly;
    }


}
