package com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9;

import com.flowpowered.nbt.*;
import com.infernalsuite.aswm.serialization.SlimeWorldReader;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.upgrade.*;
import com.infernalsuite.aswm.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.aswm.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.aswm.skeleton.SlimeChunkSkeleton;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import com.infernalsuite.aswm.api.world.SlimeWorld;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

class v1_v9SlimeConverter implements SlimeWorldReader<v1_9SlimeWorld> {

    public static final Map<Byte, Upgrade> UPGRADES = new HashMap<>();

    static {
        UPGRADES.put((byte) 0x02, new v1_9WorldUpgrade());
        UPGRADES.put((byte) 0x03, new v1_11WorldUpgrade());
        UPGRADES.put((byte) 0x04, new v1_13WorldUpgrade());
        UPGRADES.put((byte) 0x05, new v1_14WorldUpgrade());
        UPGRADES.put((byte) 0x06, new v1_16WorldUpgrade());
        UPGRADES.put((byte) 0x07, new v1_17WorldUpgrade());
        UPGRADES.put((byte) 0x08, new v1_18WorldUpgrade());
    }

    @Override
    public SlimeWorld readFromData(v1_9SlimeWorld data) {
        int dataVersion = upgradeWorld(data);

        Long2ObjectMap<SlimeChunk> chunks = new Long2ObjectOpenHashMap<>();
        for (Long2ObjectMap.Entry<v1_9SlimeChunk> entry : data.chunks.long2ObjectEntrySet()) {
            v1_9SlimeChunk slimeChunk = entry.getValue();

            SlimeChunkSection[] sections = new SlimeChunkSection[slimeChunk.sections.length];
            for (int i = 0; i < sections.length; i++) {
                v1_9SlimeChunkSection dataSection = slimeChunk.sections[i];
                if (dataSection != null) {
                    // I'm not sure which upgrader should handle this, so I'm leaving it here
                    if (dataSection.biomeTag != null) {
                        ListTag<StringTag> palette = (ListTag<StringTag>) dataSection.biomeTag.getValue().get("palette");

                        ArrayList<StringTag> newPalette = new ArrayList<StringTag>();
                        if (palette != null) {
                            for (StringTag stringTag : palette.getValue()) {
                                // air is no longer a valid biome, I'm not sure when this changed,
                                // so I cannot pick the proper upgrader to place it in.
                                if (stringTag.getValue().equals("minecraft:air")) continue;
                                newPalette.add(stringTag);
                            }
                        }

                        if (palette == null || palette.getValue().isEmpty()) {
                            newPalette.add(new StringTag(null, "minecraft:plains"));
                        }

                        dataSection.biomeTag.getValue().put("palette", new ListTag<>("palette", TagType.TAG_STRING, newPalette));
                    }

                    sections[i] = new SlimeChunkSectionSkeleton(
                            // SlimeChunkConverter can handle null blockState, but cannot handle empty blockState
                            dataSection.blockStatesTag.getValue().isEmpty() ? null : dataSection.blockStatesTag,
                            dataSection.biomeTag,
                            dataSection.blockLight,
                            dataSection.skyLight
                    );
                } else {
                    sections[i] = new SlimeChunkSectionSkeleton(
                            null,
                            null,
                            null,
                            null
                    );
                }

            }
            // TODO:
            //    slimeChunk.minY,
            //    slimeChunk.maxY,

            chunks.put(entry.getLongKey(), new SlimeChunkSkeleton(
                    slimeChunk.x,
                    slimeChunk.z,
                    sections,
                    slimeChunk.heightMap,
                    slimeChunk.tileEntities,
                    slimeChunk.entities,
                    new CompoundTag("", new CompoundMap()),
                    slimeChunk.upgradeData
            ));
        }


        return new SkeletonSlimeWorld(
                data.worldName,
                data.loader,
                data.readOnly,
                chunks,
                data.extraCompound,
                data.propertyMap,
                dataVersion
        );
    }


    public static int upgradeWorld(v1_9SlimeWorld world) {
        byte upgradeTo = 0x08; // Last version
        int dataVersion = 3120; // MCVersions.V1_19_2

        for (byte ver = (byte) (world.version + 1); ver <= upgradeTo; ver++) {
            Upgrade upgrade = UPGRADES.get(ver);

            if (upgrade == null) {
                Logger.getLogger("v1_9WorldUpgrader").warning("Missing world upgrader for version " + ver + ". World will not be upgraded.");
                continue;
            }

            upgrade.upgrade(world);

            if (ver == 0x08) {
                dataVersion = 2975;
            }
        }

        world.version = 0x09;
        return dataVersion;
    }
}
