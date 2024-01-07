package com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.upgrade;

import com.flowpowered.nbt.*;
import com.infernalsuite.aswm.api.SlimeNMSBridge;
import com.infernalsuite.aswm.api.utils.NibbleArray;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.Upgrade;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.v1_9SlimeChunk;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.v1_9SlimeChunkSection;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.v1_9SlimeWorld;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class v1_13WorldUpgrade implements Upgrade {

    @Override
    public void upgrade(v1_9SlimeWorld world) {
        Logger.getLogger("v1_13WorldUpgrade").warning("Updating world to the 1.13 format. This may take a while.");

        List<v1_9SlimeChunk> chunks = new ArrayList<>(world.chunks.values());
        long lastMessage = -1;

        for (int i = 0; i < chunks.size(); i++) {
            v1_9SlimeChunk chunk = chunks.get(i);

            // The world upgrade process is a very complex task, and there's already a
            // built-in upgrade tool inside the server, so we can simply use it
            CompoundTag globalTag = new CompoundTag("", new CompoundMap());
            globalTag.getValue().put("DataVersion", new IntTag("DataVersion", 1343));

            CompoundTag chunkTag = new CompoundTag("Level", new CompoundMap());

            chunkTag.getValue().put("xPos", new IntTag("xPos", chunk.x));
            chunkTag.getValue().put("zPos", new IntTag("zPos", chunk.z));
            chunkTag.getValue().put("Sections", serializeSections(chunk.sections));
            chunkTag.getValue().put("Entities", new ListTag<>("Entities", TagType.TAG_COMPOUND, chunk.entities));
            chunkTag.getValue().put("TileEntities", new ListTag<>("TileEntities", TagType.TAG_COMPOUND, chunk.tileEntities));
            chunkTag.getValue().put("TileTicks", new ListTag<>("TileTicks", TagType.TAG_COMPOUND, new ArrayList<>()));
            chunkTag.getValue().put("TerrainPopulated", new ByteTag("TerrainPopulated", (byte) 1));
            chunkTag.getValue().put("LightPopulated", new ByteTag("LightPopulated", (byte) 1));

            globalTag.getValue().put("Level", chunkTag);

            globalTag = SlimeNMSBridge.instance().convertChunkTo1_13(globalTag);
            chunkTag = globalTag.getAsCompoundTag("Level").get();

            // Chunk sections
            v1_9SlimeChunkSection[] newSections = new v1_9SlimeChunkSection[16];
            ListTag<CompoundTag> serializedSections = (ListTag<CompoundTag>) chunkTag.getAsListTag("Sections").get();

            for (CompoundTag sectionTag : serializedSections.getValue()) {
                ListTag<CompoundTag> palette = (ListTag<CompoundTag>) sectionTag.getAsListTag("Palette").get();
                long[] blockStates = sectionTag.getLongArrayValue("BlockStates").get();

                NibbleArray blockLight = new NibbleArray(sectionTag.getByteArrayValue("BlockLight").get());
                NibbleArray skyLight = new NibbleArray(sectionTag.getByteArrayValue("SkyLight").get());

                int index = sectionTag.getIntValue("Y").get();

                v1_9SlimeChunkSection section = new v1_9SlimeChunkSection(null, null, palette, blockStates, null, null, blockLight, skyLight);
                newSections[index] = section;
            }

            // Biomes
            int[] newBiomes = new int[256];

            for (int index = 0; index < chunk.biomes.length; index++) {
                newBiomes[index] = chunk.biomes[index] & 255;
            }

            chunk.sections = newSections;
            chunk.biomes = newBiomes;

            // Upgrade data
            chunk.upgradeData = chunkTag.getAsCompoundTag("UpgradeData").orElse(null);

            int done = i + 1;
            if (done == chunks.size()) {
                Logger.getLogger("v1_13WorldUpgrade").info("World successfully converted to the 1.13 format!");
            } else if (System.currentTimeMillis() - lastMessage > 1000) {
                int percentage = (done * 100) / chunks.size();
                Logger.getLogger("v1_13WorldUpgrade").info("Converting world... " + percentage + "%");
                lastMessage = System.currentTimeMillis();
            }
        }
    }

    private ListTag<CompoundTag> serializeSections(v1_9SlimeChunkSection[] sections) {
        ListTag<CompoundTag> sectionList = new ListTag<>("Sections", TagType.TAG_COMPOUND, new ArrayList<>());

        for (int i = 0; i < sections.length; i++) {
            v1_9SlimeChunkSection section = sections[i];

            if (section != null) {
                CompoundTag sectionTag = new CompoundTag(i + "", new CompoundMap());

                sectionTag.getValue().put("Y", new IntTag("Y", i));
                sectionTag.getValue().put("Blocks", new ByteArrayTag("Blocks", section.blocks));
                sectionTag.getValue().put("Data", new ByteArrayTag("Data", section.data.getBacking()));
                sectionTag.getValue().put("BlockLight", new ByteArrayTag("Data", section.blockLight.getBacking()));
                sectionTag.getValue().put("SkyLight", new ByteArrayTag("Data", section.skyLight.getBacking()));

                sectionList.getValue().add(sectionTag);
            }
        }

        return sectionList;
    }
}
