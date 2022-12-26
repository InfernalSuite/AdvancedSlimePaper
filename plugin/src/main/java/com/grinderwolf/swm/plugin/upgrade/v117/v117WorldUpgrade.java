package com.grinderwolf.swm.plugin.upgrade.v117;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.StringTag;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.nms.world.SlimeLoadedWorld;
import com.grinderwolf.swm.plugin.upgrade.Upgrade;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class v117WorldUpgrade implements Upgrade {

    @Override
    public void upgrade(SlimeLoadedWorld world) {
        for (SlimeChunk chunk : new ArrayList<>(world.getChunks().values())) {
            for (SlimeChunkSection section : chunk.getSections()) {
                if (section == null) {
                    continue;
                }

                List<CompoundTag> palette = section.getPalette().getValue();

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
