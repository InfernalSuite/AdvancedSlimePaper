package com.infernalsuite.asp.serialization.slime.reader.impl.v1_9;

import com.infernalsuite.asp.api.SlimeNMSBridge;
import com.infernalsuite.asp.serialization.SlimeWorldReader;
import com.infernalsuite.asp.serialization.slime.reader.impl.v1_9.upgrade.*;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.api.world.SlimeWorld;

import com.infernalsuite.asp.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.asp.skeleton.SlimeChunkSkeleton;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.util.HashMap;
import java.util.Map;

class v1_v9SlimeConverter implements SlimeWorldReader<v1_9SlimeWorld> {

    public static final Map<Byte, Upgrade> UPGRADES = new HashMap<>();

    static {
        UPGRADES.put((byte) 0x04, new v1_13WorldUpgrade());
        UPGRADES.put((byte) 0x06, new v1_16WorldUpgrade());
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
                    sections[i] = new SlimeChunkSectionSkeleton(
                            // SlimeChunkConverter can handle null blockState, but cannot handle empty blockState
                            dataSection.blockStatesTag.isEmpty() ? null : dataSection.blockStatesTag,
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
                    new HashMap<>(),
                    slimeChunk.upgradeData,
                    null,
                    null,
                    null
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

        for (byte ver = (byte) (world.version + 1); ver <= upgradeTo; ver++) {
            Upgrade upgrade = UPGRADES.get(ver);

            if (upgrade == null) {
                continue;
            }

            upgrade.upgrade(world, SlimeNMSBridge.instance().getSlimeDataConverter());
            world.version=ver;
        }
        return world.getDataVersion();
    }
}
