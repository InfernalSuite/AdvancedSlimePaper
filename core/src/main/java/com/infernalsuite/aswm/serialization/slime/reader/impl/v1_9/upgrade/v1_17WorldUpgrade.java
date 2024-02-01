package com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.upgrade;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.StringTag;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.Upgrade;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.v1_9SlimeChunk;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.v1_9SlimeChunkSection;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.v1_9SlimeWorld;

import java.util.List;
import java.util.Optional;

public class v1_17WorldUpgrade implements Upgrade {

    @Override
    public void upgrade(v1_9SlimeWorld world) {
        for (v1_9SlimeChunk chunk : world.chunks.values()) {
            for (v1_9SlimeChunkSection section : chunk.sections) {
                if (section == null) {
                    continue;
                }

                List<CompoundTag> palette = section.palette.getValue();

                for (CompoundTag blockTag : palette) {
                    Optional<String> name = blockTag.getStringValue("Name");
                    CompoundMap map = blockTag.getValue();

                    // CauldronRenameFix
                    if (name.equals(Optional.of("minecraft:cauldron"))) {
                        Optional<CompoundTag> properties = blockTag.getAsCompoundTag("Properties");
                        if (properties.isPresent()) {
                            String waterLevel = blockTag.getStringValue("level").orElse("0");
                            if (waterLevel.equals("0")) {
                                map.remove("Properties");
                            } else {
                                map.put("Name", new StringTag("Name", "minecraft:water_cauldron"));
                            }
                        }
                    }

                    // Renamed grass path item to dirt path
                    if (name.equals(Optional.of("minecraft:grass_path"))) {
                        map.put("Name", new StringTag("Name", "minecraft:dirt_path"));
                    }
                }
            }
        }
    }

}