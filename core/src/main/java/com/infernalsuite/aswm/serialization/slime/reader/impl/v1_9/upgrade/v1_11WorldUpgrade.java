package com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.upgrade;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.StringTag;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.Upgrade;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.v1_9SlimeChunk;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.v1_9SlimeWorld;

import java.util.HashMap;
import java.util.Map;

public class v1_11WorldUpgrade implements Upgrade {

    private static Map<String, String> oldToNewMap = new HashMap<>();
    private static Map<String, String> newToOldMap = new HashMap<>();

    static {
        rename("Furnace", "minecraft:furnace");
        rename("Chest", "minecraft:chest");
        rename("EnderChest", "minecraft:ender_chest");
        rename("RecordPlayer", "minecraft:jukebox");
        rename("Trap", "minecraft:dispenser");
        rename("Dropper", "minecraft:dropper");
        rename("Sign", "minecraft:sign");
        rename("MobSpawner", "minecraft:mob_spawner");
        rename("Music", "minecraft:noteblock");
        rename("Piston", "minecraft:piston");
        rename("Cauldron", "minecraft:brewing_stand");
        rename("EnchantTable", "minecraft:enchanting_table");
        rename("Airportal", "minecraft:end_portal");
        rename("Beacon", "minecraft:beacon");
        rename("Skull", "minecraft:skull");
        rename("DLDetector", "minecraft:daylight_detector");
        rename("Hopper", "minecraft:hopper");
        rename("Comparator", "minecraft:comparator");
        rename("FlowerPot", "minecraft:flower_pot");
        rename("Banner", "minecraft:banner");
        rename("Structure", "minecraft:structure_block");
        rename("EndGateway", "minecraft:end_gateway");
        rename("Control", "minecraft:command_block");
        rename(null, "minecraft:bed"); // Patch for issue s#62
    }

    private static void rename(String oldName, String newName) {
        if (oldName != null) {
            oldToNewMap.put(oldName, newName);
        }

        newToOldMap.put(newName, oldName);
    }

    @Override
    public void upgrade(v1_9SlimeWorld world) {
        // 1.11 changed the way Tile Entities are named
        for (v1_9SlimeChunk chunk : world.chunks.values()) {
            for (CompoundTag entityTag : chunk.tileEntities) {
                String oldType = entityTag.getAsStringTag("id").get().getValue();
                String newType = oldToNewMap.get(oldType);

                if (newType == null) {
                    if (newToOldMap.containsKey(oldType)) { // Maybe it's in the new format for some reason?
                        continue;
                    }

                    throw new IllegalStateException("Failed to find 1.11 upgrade for tile entity " + oldType);
                }

                entityTag.getValue().put("id", new StringTag("id", newType));
            }
        }
    }
}
