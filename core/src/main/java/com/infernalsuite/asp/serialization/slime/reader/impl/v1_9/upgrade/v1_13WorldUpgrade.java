package com.infernalsuite.asp.serialization.slime.reader.impl.v1_9.upgrade;

import com.infernalsuite.asp.api.SlimeDataConverter;
import com.infernalsuite.asp.api.SlimeNMSBridge;
import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.serialization.slime.reader.impl.v1_9.Upgrade;
import com.infernalsuite.asp.serialization.slime.reader.impl.v1_9.v1_9SlimeChunk;
import com.infernalsuite.asp.serialization.slime.reader.impl.v1_9.v1_9SlimeChunkSection;
import com.infernalsuite.asp.serialization.slime.reader.impl.v1_9.v1_9SlimeWorld;
import net.kyori.adventure.nbt.*;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class v1_13WorldUpgrade implements Upgrade {

    private static final int DATA_VERSION = 1631;

    @Override
    public void upgrade(v1_9SlimeWorld world, SlimeDataConverter slimeDataConverter) {
        Logger.getLogger("v1_13WorldUpgrade").warning("Updating world to the 1.13 format. This may take a while.");


        List<v1_9SlimeChunk> chunks = new ArrayList<>(world.chunks.values());
        long lastMessage = -1;

        for (int i = 0; i < chunks.size(); i++) {
            v1_9SlimeChunk chunk = chunks.get(i);

            //Make sure that entities and tile entities are up-to-date with pre-1.13
            chunk.tileEntities = slimeDataConverter.convertTileEntities(chunk.tileEntities, world.getDataVersion(), 1343);
            chunk.entities = slimeDataConverter.convertEntities(chunk.entities, world.getDataVersion(), 1343);

            // The world upgrade process is a very complex task, and there's already a
            // built-in upgrade tool inside the server, so we can simply use it
            CompoundBinaryTag.Builder globalTag = CompoundBinaryTag.builder();
            globalTag.put("DataVersion", IntBinaryTag.intBinaryTag(1343));


            CompoundBinaryTag.Builder chunkTag = CompoundBinaryTag.builder();

            chunkTag.put("xPos", IntBinaryTag.intBinaryTag(chunk.x));
            chunkTag.put("zPos", IntBinaryTag.intBinaryTag(chunk.z));
            chunkTag.put("Sections", serializeSections(chunk.sections));
            chunkTag.put("Entities", ListBinaryTag.builder().add(chunk.entities).build());
            chunkTag.put("TileEntities", ListBinaryTag.builder().add(chunk.tileEntities).build());
            chunkTag.put("TileTicks", ListBinaryTag.empty());
            chunkTag.put("TerrainPopulated", ByteBinaryTag.byteBinaryTag((byte) 1));
            chunkTag.put("LightPopulated", ByteBinaryTag.byteBinaryTag((byte) 1));

            globalTag.put("Level", chunkTag.build());

            CompoundBinaryTag convertedTag = slimeDataConverter.convertChunkTo1_13(globalTag.build());
            CompoundBinaryTag convertedChunk = convertedTag.getCompound("Level");

            // Chunk sections
            v1_9SlimeChunkSection[] newSections = new v1_9SlimeChunkSection[16];
            ListBinaryTag serializedSections = convertedChunk.getList("Sections");

            for (BinaryTag sectionTag : serializedSections) {
                CompoundBinaryTag sectionCompound = (CompoundBinaryTag) sectionTag;
                ListBinaryTag palette = sectionCompound.getList("Palette");
                long[] blockStates = sectionCompound.getLongArray("BlockStates");

                NibbleArray blockLight = new NibbleArray(sectionCompound.getByteArray("BlockLight"));
                NibbleArray skyLight = new NibbleArray(sectionCompound.getByteArray("SkyLight"));

                int index = sectionCompound.getInt("Y");

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
            chunk.upgradeData = convertedChunk.getCompound("UpgradeData");

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

    private ListBinaryTag serializeSections(v1_9SlimeChunkSection[] sections) {
        ListBinaryTag.@NotNull Builder<BinaryTag> builder = ListBinaryTag.builder();

        for (int i = 0; i < sections.length; i++) {
            v1_9SlimeChunkSection section = sections[i];

            if (section != null) {
                CompoundBinaryTag.Builder sectionTag = CompoundBinaryTag.builder();

                sectionTag.put("Y", IntBinaryTag.intBinaryTag(i));
                sectionTag.put("Blocks", ByteArrayBinaryTag.byteArrayBinaryTag(section.blocks));
                sectionTag.put("Data", ByteArrayBinaryTag.byteArrayBinaryTag(section.data.getBacking()));
                sectionTag.put("BlockLight", ByteArrayBinaryTag.byteArrayBinaryTag(section.blockLight.getBacking()));
                sectionTag.put("SkyLight", ByteArrayBinaryTag.byteArrayBinaryTag(section.skyLight.getBacking()));

                builder.add(sectionTag.build());
            }
        }

        return builder.build();
    }
}
