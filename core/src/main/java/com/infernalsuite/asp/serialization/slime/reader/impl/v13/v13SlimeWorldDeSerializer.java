package com.infernalsuite.asp.serialization.slime.reader.impl.v13;

import com.github.luben.zstd.Zstd;
import com.infernalsuite.asp.SlimeLogger;
import com.infernalsuite.asp.Util;
import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.skeleton.SlimeChunkSkeleton;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.adventure.nbt.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class v13SlimeWorldDeSerializer implements com.infernalsuite.asp.serialization.slime.reader.VersionedByteSlimeWorldReader<SlimeWorld> {

    public static final int ARRAY_SIZE = 16 * 16 * 16 / (8 / 4);

    @Override
    public SlimeWorld deserializeWorld(byte version, @Nullable SlimeLoader loader, String worldName, DataInputStream dataStream, SlimePropertyMap propertyMap, boolean readOnly) throws IOException, CorruptedWorldException, NewerFormatException {
        int worldVersion = dataStream.readInt();
        byte additionalWorldData = dataStream.readByte();

        byte[] chunkBytes = readCompressed(dataStream);
        Long2ObjectMap<SlimeChunk> chunks = readChunks(propertyMap, additionalWorldData, chunkBytes);

        byte[] extraTagBytes = readCompressed(dataStream);
        CompoundBinaryTag extraTag = readCompound(extraTagBytes);

        ConcurrentMap<String, BinaryTag> extraData = new ConcurrentHashMap<>();
        extraTag.forEach(entry -> extraData.put(entry.getKey(), entry.getValue()));

        SlimePropertyMap worldPropertyMap = propertyMap;
        if (extraData.containsKey("properties")) {
            CompoundBinaryTag serializedSlimeProperties = (CompoundBinaryTag) extraData.get("properties");
            worldPropertyMap = SlimePropertyMap.fromCompound(serializedSlimeProperties);
            worldPropertyMap.merge(propertyMap);
        }

        return new com.infernalsuite.asp.skeleton.SkeletonSlimeWorld(worldName, loader, readOnly, chunks, extraData, worldPropertyMap, worldVersion);
    }

    private static Long2ObjectMap<SlimeChunk> readChunks(SlimePropertyMap slimePropertyMap, byte additionalWorldData, byte[] chunkBytes) throws IOException {
        Long2ObjectMap<SlimeChunk> chunkMap = new Long2ObjectOpenHashMap<>();
        DataInputStream chunkData = new DataInputStream(new ByteArrayInputStream(chunkBytes));

        int chunks = chunkData.readInt();
        for (int i = 0; i < chunks; i++) {
            // ChunkPos
            int x = chunkData.readInt();
            int z = chunkData.readInt();

            // Sections
            int sectionAmount = slimePropertyMap.getValue(SlimeProperties.CHUNK_SECTION_MAX) - slimePropertyMap.getValue(SlimeProperties.CHUNK_SECTION_MIN) + 1;
            SlimeChunkSection[] chunkSections = new SlimeChunkSection[sectionAmount];

            int sectionCount = chunkData.readInt();
            for (int sectionId = 0; sectionId < sectionCount; sectionId++) {
                byte sectionFlags = chunkData.readByte();

                // Block Light Nibble Array
                NibbleArray blockLightArray;
                if ((sectionFlags & 1) == 1) {
                    byte[] blockLightByteArray = new byte[ARRAY_SIZE];
                    chunkData.read(blockLightByteArray);
                    blockLightArray = new NibbleArray(blockLightByteArray);
                } else {
                    blockLightArray = null;
                }

                // Sky Light Nibble Array
                NibbleArray skyLightArray;
                if (((sectionFlags >> 1) & 1) == 1) {
                    byte[] skyLightByteArray = new byte[ARRAY_SIZE];
                    chunkData.read(skyLightByteArray);
                    skyLightArray = new NibbleArray(skyLightByteArray);
                } else {
                    skyLightArray = null;
                }

                // Block Data
                byte[] blockStateData = new byte[chunkData.readInt()];
                chunkData.read(blockStateData);
                CompoundBinaryTag blockStateTag = readCompound(blockStateData);

                // Biome Data
                byte[] biomeData = new byte[chunkData.readInt()];
                chunkData.read(biomeData);
                CompoundBinaryTag biomeTag = readCompound(biomeData);

                chunkSections[sectionId] = new com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton(blockStateTag, biomeTag, blockLightArray, skyLightArray);
            }

            // HeightMaps
            byte[] heightMapData = new byte[chunkData.readInt()];
            chunkData.read(heightMapData);
            CompoundBinaryTag heightMaps = readCompound(heightMapData);

            CompoundBinaryTag poiChunk = null;
            if(v13AdditionalWorldData.POI_CHUNKS.isSet(additionalWorldData)) {
                byte[] poiData = new byte[chunkData.readInt()];
                chunkData.read(poiData);
                poiChunk = readCompound(poiData);
            }

            ListBinaryTag blockTicks = null;
            if(v13AdditionalWorldData.BLOCK_TICKS.isSet(additionalWorldData)) {
                byte[] blockTickData = new byte[chunkData.readInt()];
                chunkData.read(blockTickData);
                CompoundBinaryTag tag = readCompound(blockTickData);
                blockTicks = tag.getList("block_ticks", BinaryTagTypes.COMPOUND);
            }
            ListBinaryTag fluidTicks = null;
            if(v13AdditionalWorldData.FLUID_TICKS.isSet(additionalWorldData)) {
                byte[] fluidTickData = new byte[chunkData.readInt()];
                chunkData.read(fluidTickData);
                CompoundBinaryTag tag = readCompound(fluidTickData);
                fluidTicks = tag.getList("fluid_ticks", BinaryTagTypes.COMPOUND);
            }

            int countOfUnsupportedData = v13AdditionalWorldData.countUnsupportedFlags(additionalWorldData);
            for (int i1 = 0; i1 < countOfUnsupportedData; i1++) {
                byte[] randomData = new byte[chunkData.readInt()];
                chunkData.read(randomData);
            }
            if(countOfUnsupportedData > 0) {
                SlimeLogger.warn("Unsupported additional world data found in chunk " + x + ", " + z + ". This should not cause any issues, however this data will be lost on save.");
            }

            // Tile Entities

            byte[] tileEntitiesRaw = read(chunkData);
            List<CompoundBinaryTag> tileEntities;
            CompoundBinaryTag tileEntitiesCompound = readCompound(tileEntitiesRaw);
            if (tileEntitiesCompound.isEmpty()) {
                tileEntities = Collections.emptyList();
            } else {
                tileEntities = tileEntitiesCompound.getList("tileEntities", BinaryTagTypes.COMPOUND).stream()
                        .map(tag -> (CompoundBinaryTag) tag)
                        .toList();
            }

            // Entities

            byte[] entitiesRaw = read(chunkData);
            List<CompoundBinaryTag> entities;
            CompoundBinaryTag entitiesCompound = readCompound(entitiesRaw);
            if (entitiesCompound.isEmpty()) {
                entities = Collections.emptyList();
            } else {
                entities = entitiesCompound.getList("entities", BinaryTagTypes.COMPOUND).stream()
                        .map(tag -> (CompoundBinaryTag) tag)
                        .toList();
            }

            // Extra Tag
            byte[] rawExtra = read(chunkData);
            CompoundBinaryTag extra = readCompound(rawExtra);

            Map<String, BinaryTag> extraData = new HashMap<>();
            extra.forEach(entry -> extraData.put(entry.getKey(), entry.getValue()));

            chunkMap.put(Util.chunkPosition(x, z), new SlimeChunkSkeleton(x, z, chunkSections, heightMaps, tileEntities, entities, extraData, null, poiChunk, blockTicks, fluidTicks));
        }
        return chunkMap;
    }

    private static byte[] readCompressed(DataInputStream stream) throws IOException {
        int compressedLength = stream.readInt();
        int decompressedLength = stream.readInt();
        byte[] compressedData = new byte[compressedLength];
        byte[] decompressedData = new byte[decompressedLength];
        stream.read(compressedData);
        Zstd.decompress(decompressedData, compressedData);
        return decompressedData;
    }

    private static byte[] read(DataInputStream stream) throws IOException {
        int length = stream.readInt();
        byte[] data = new byte[length];
        stream.read(data);
        return data;
    }

    private static @NotNull CompoundBinaryTag readCompound(byte[] tagBytes) throws IOException {
        if (tagBytes.length == 0) return CompoundBinaryTag.empty();

        return BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(tagBytes));
    }
}
