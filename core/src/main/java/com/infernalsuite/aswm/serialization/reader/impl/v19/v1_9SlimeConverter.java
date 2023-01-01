package com.infernalsuite.aswm.serialization.reader.impl.v19;

import com.flowpowered.nbt.CompoundTag;
import com.infernalsuite.aswm.ChunkPos;
import com.infernalsuite.aswm.serialization.reader.impl.SlimeConverter;
import com.infernalsuite.aswm.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.aswm.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.aswm.skeleton.SlimeChunkSkeleton;
import com.infernalsuite.aswm.world.SlimeChunk;
import com.infernalsuite.aswm.world.SlimeChunkSection;
import com.infernalsuite.aswm.world.SlimeWorld;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

class v1_9SlimeConverter implements SlimeConverter<v1_9SlimeWorld> {

    private static final Map<Byte, Upgrade> upgrades = new HashMap<>();

    static {
        upgrades.put((byte) 0x06, new v1_16WorldUpgrade());
        upgrades.put((byte) 0x07, new v117WorldUpgrade());
        upgrades.put((byte) 0x08, new v118WorldUpgrade());
    }

    @Override
    public SlimeWorld runConversion(v1_9SlimeWorld data) {
        upgradeWorld(data);

        Map<ChunkPos, SlimeChunk> chunks = new HashMap<>();
        List<CompoundTag> entities = new ArrayList<>();
        for (Map.Entry<ChunkPos, v1_9SlimeChunk> entry : data.chunks.entrySet()) {
            v1_9SlimeChunk slimeChunk = entry.getValue();

            SlimeChunkSection[] sections = new SlimeChunkSection[slimeChunk.sections.length];
            for (int i = 0; i < sections.length; i++) {
                v1_9SlimeChunkSection dataSection = slimeChunk.sections[i];
                sections[i] = new SlimeChunkSectionSkeleton(
                        dataSection.blockStatesTag,
                        dataSection.biomeTag,
                        dataSection.blockLight,
                        dataSection.skyLight
                );
            }
            // TODO:
            //    slimeChunk.minY,
            //    slimeChunk.maxY,

            chunks.put(entry.getKey(), new SlimeChunkSkeleton(
                    slimeChunk.x,
                    slimeChunk.z,
                    sections,
                    slimeChunk.heightMap,
                    slimeChunk.tileEntities
            ));
            entities.addAll(slimeChunk.entities);
        }


        return new SkeletonSlimeWorld(
                data.worldName,
                data.loader,
                chunks,
                data.extraCompound,
                data.propertyMap,
                entities,
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
