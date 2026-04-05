package com.infernalsuite.asp.serialization.slime.reader.impl.v13;

import com.github.luben.zstd.ZstdInputStream;
import com.infernalsuite.asp.SlimeLogger;
import com.infernalsuite.asp.util.Util;
import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.exceptions.NewerFormatException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.util.LimitedInputStream;
import com.infernalsuite.asp.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.asp.skeleton.SlimeChunkSkeleton;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.adventure.nbt.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
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

        DataInputStream chunkBytes = openCompressedStream(dataStream);
        Long2ObjectMap<SlimeChunk> chunks = readChunks(propertyMap, additionalWorldData, chunkBytes);

        CompoundBinaryTag extraTag = readCompressedCompound(dataStream);

        ConcurrentMap<String, BinaryTag> extraData = new ConcurrentHashMap<>();
        extraTag.forEach(entry -> extraData.put(entry.getKey(), entry.getValue()));

        SlimePropertyMap worldPropertyMap = propertyMap;
        if (extraData.containsKey("properties")) {
            CompoundBinaryTag serializedSlimeProperties = (CompoundBinaryTag) extraData.get("properties");
            worldPropertyMap = SlimePropertyMap.fromCompound(serializedSlimeProperties);
            worldPropertyMap.merge(propertyMap);
        }


        chunkBytes.close();
        dataStream.close();
        return new SkeletonSlimeWorld(worldName, loader, readOnly, chunks, extraData, worldPropertyMap, worldVersion);
    }

    private static Long2ObjectMap<SlimeChunk> readChunks(SlimePropertyMap slimePropertyMap, byte additionalWorldData, DataInputStream chunkData) throws IOException {
        Long2ObjectMap<SlimeChunk> chunkMap = new Long2ObjectOpenHashMap<>();

        int chunks = chunkData.readInt();
        for (int i = 0; i < chunks; i++) {
            // ChunkPos
            int x = chunkData.readInt();
            int z = chunkData.readInt();

            // Sections
            int sectionCount = chunkData.readInt();
            SlimeChunkSection[] chunkSections = new SlimeChunkSection[sectionCount];
            for (int sectionId = 0; sectionId < sectionCount; sectionId++) {
                byte sectionFlags = chunkData.readByte();

                // Block Light Nibble Array
                NibbleArray blockLightArray;
                if ((sectionFlags & 1) == 1) {
                    byte[] blockLightByteArray = new byte[ARRAY_SIZE];
                    chunkData.readFully(blockLightByteArray);
                    blockLightArray = new NibbleArray(blockLightByteArray);
                } else {
                    blockLightArray = null;
                }

                // Sky Light Nibble Array
                NibbleArray skyLightArray;
                if (((sectionFlags >> 1) & 1) == 1) {
                    byte[] skyLightByteArray = new byte[ARRAY_SIZE];
                    chunkData.readFully(skyLightByteArray);
                    skyLightArray = new NibbleArray(skyLightByteArray);
                } else {
                    skyLightArray = null;
                }

                // Block Data
                CompoundBinaryTag blockStateTag = readLimitedCompound(chunkData);

                // Biome Data
                CompoundBinaryTag biomeTag = readLimitedCompound(chunkData);

                chunkSections[sectionId] = new SlimeChunkSectionSkeleton(blockStateTag, biomeTag, blockLightArray, skyLightArray);
            }

            // HeightMaps
            CompoundBinaryTag heightMaps = readLimitedCompound(chunkData);

            CompoundBinaryTag poiChunk = null;
            if(v13AdditionalWorldData.POI_CHUNKS.isSet(additionalWorldData)) {
                poiChunk = readLimitedCompound(chunkData);
            }

            ListBinaryTag blockTicks = null;
            if(v13AdditionalWorldData.BLOCK_TICKS.isSet(additionalWorldData)) {
                CompoundBinaryTag tag = readLimitedCompound(chunkData);
                blockTicks = tag.getList("block_ticks", BinaryTagTypes.COMPOUND);
            }
            ListBinaryTag fluidTicks = null;
            if(v13AdditionalWorldData.FLUID_TICKS.isSet(additionalWorldData)) {
                CompoundBinaryTag tag = readLimitedCompound(chunkData);
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

            List<CompoundBinaryTag> tileEntities;
            CompoundBinaryTag tileEntitiesCompound = readLimitedCompound(chunkData);
            if (tileEntitiesCompound.isEmpty()) {
                tileEntities = Collections.emptyList();
            } else {
                tileEntities = tileEntitiesCompound.getList("tileEntities", BinaryTagTypes.COMPOUND).stream()
                        .map(tag -> (CompoundBinaryTag) tag)
                        .toList();
            }

            // Entities

            List<CompoundBinaryTag> entities;
            CompoundBinaryTag entitiesCompound = readLimitedCompound(chunkData);
            if (entitiesCompound.isEmpty()) {
                entities = Collections.emptyList();
            } else {
                entities = entitiesCompound.getList("entities", BinaryTagTypes.COMPOUND).stream()
                        .map(tag -> (CompoundBinaryTag) tag)
                        .toList();
            }

            // Extra Tag
            CompoundBinaryTag extra = readLimitedCompound(chunkData);

            Map<String, BinaryTag> extraData = new HashMap<>();
            extra.forEach(entry -> extraData.put(entry.getKey(), entry.getValue()));

            chunkMap.put(Util.chunkPosition(x, z), new SlimeChunkSkeleton(x, z, chunkSections, heightMaps, tileEntities, entities, extraData, null, poiChunk, blockTicks, fluidTicks));
        }
        return chunkMap;
    }

    private static DataInputStream openCompressedStream(DataInputStream stream) throws IOException {
        int compressedLength = stream.readInt();
        stream.readInt(); //Decompressed length, legacy

        LimitedInputStream limitedInputStream = new LimitedInputStream(stream, compressedLength);
        ZstdInputStream inputStream = new ZstdInputStream(limitedInputStream);
        return new DataInputStream(new BufferedInputStream(inputStream));
    }

    private static @NotNull CompoundBinaryTag readLimitedCompound(DataInputStream stream) throws IOException {
        int length = stream.readInt();
        if(length == 0) return CompoundBinaryTag.empty();

        LimitedInputStream limitedInputStream = new LimitedInputStream(stream, length);

        //Avoid a buffered input stream by casting to DataInput. Buffered Input Streams make the memory
        //usage explode (e.g. with buffered streams here 1,3gb; with a data input directly: 300mb)
        CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read((DataInput) new DataInputStream(limitedInputStream));

        //binary tag reading does not guarantee that the buffer is fully read. If we don't do this,
        //we might error out later
        limitedInputStream.drainRemaining();
        return tag;
    }

    private static @NotNull CompoundBinaryTag readCompressedCompound(DataInputStream stream) throws IOException {
        int compressedLength = stream.readInt();
        int decompressedLength = stream.readInt();

        if(decompressedLength == 0) return CompoundBinaryTag.empty();

        LimitedInputStream limitedInputStream = new LimitedInputStream(stream, compressedLength);
        try(ZstdInputStream zstd = new ZstdInputStream(limitedInputStream)) {

            //Avoid a buffered input stream by casting to DataInput. Buffered Input Streams make the memory
            //usage explode (e.g. with buffered streams here 1,3gb; with a data input directly: 300mb)
            CompoundBinaryTag tag = BinaryTagIO.unlimitedReader().read((DataInput) new DataInputStream(zstd));

            //binary tag reading does not guarantee that the buffer is fully read. If we don't do this,
            //we might error out later
            byte[] buffer = new byte[512];
            while (zstd.read(buffer) != -1) {}

            return tag;
        }
    }

}
