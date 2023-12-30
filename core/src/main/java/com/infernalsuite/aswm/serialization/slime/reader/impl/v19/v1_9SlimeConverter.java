package com.infernalsuite.aswm.serialization.slime.reader.impl.v19;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.infernalsuite.aswm.ChunkPos;
import com.infernalsuite.aswm.serialization.SlimeWorldReader;
import com.infernalsuite.aswm.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.aswm.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.aswm.skeleton.SlimeChunkSkeleton;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import com.infernalsuite.aswm.api.world.SlimeWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

class v1_9SlimeConverter implements SlimeWorldReader<v1_9SlimeWorld> {

    private static final Map<Byte, Upgrade> upgrades = new HashMap<>();

    static {
        upgrades.put((byte) 0x06, new v1_16WorldUpgrade());
        upgrades.put((byte) 0x07, new v117WorldUpgrade());
        upgrades.put((byte) 0x08, new v118WorldUpgrade());
    }

    @Override
    public SlimeWorld readFromData(v1_9SlimeWorld data) {
        upgradeWorld(data);

        Map<ChunkPos, SlimeChunk> chunks = new HashMap<>();
        for (Map.Entry<ChunkPos, v1_9SlimeChunk> entry : data.chunks.entrySet()) {
            v1_9SlimeChunk slimeChunk = entry.getValue();

            SlimeChunkSection[] sections = new SlimeChunkSection[slimeChunk.sections.length];
            for (int i = 0; i < sections.length; i++) {
                v1_9SlimeChunkSection dataSection = slimeChunk.sections[i];
                if (dataSection != null) {
                    sections[i] = new SlimeChunkSectionSkeleton(
                            dataSection.blockStatesTag,
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

            chunks.put(entry.getKey(), new SlimeChunkSkeleton(
                    slimeChunk.x,
                    slimeChunk.z,
                    sections,
                    slimeChunk.heightMap,
                    slimeChunk.tileEntities,
                    slimeChunk.entities,
                    new CompoundTag("", new CompoundMap())
            ));
        }


        return new SkeletonSlimeWorld(
                data.worldName,
                data.loader,
                data.readOnly,
                chunks,
                data.extraCompound,
                data.propertyMap,
                3120 // MCVersions.V1_19_2
        );
    }


    public static void upgradeWorld(v1_9SlimeWorld world) {
        byte serverVersion = 0x09; // Last version

        for (byte ver = (byte) (world.version + 1); ver <= serverVersion; ver++) {
            Upgrade upgrade = upgrades.get(ver);

            if (upgrade == null) {
                Logger.getLogger("v1_9WorldUpgrader").warning("Missing world upgrader for version " + ver + ". World will not be upgraded.");
                continue;
            }

            upgrade.upgrade(world);
        }

        world.version = serverVersion;
    }
}
