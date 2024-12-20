package com.infernalsuite.asp.serialization.slime.reader.impl.v12;

import com.github.luben.zstd.Zstd;
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
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class v12SlimeWorldDeSerializer implements com.infernalsuite.asp.serialization.slime.reader.VersionedByteSlimeWorldReader<com.infernalsuite.asp.api.world.SlimeWorld> {

    public static final int ARRAY_SIZE = 16 * 16 * 16 / (8 / 4);

    @Override
    public SlimeWorld deserializeWorld(byte version, @Nullable SlimeLoader loader, String worldName, DataInputStream dataStream, SlimePropertyMap propertyMap, boolean readOnly) throws IOException, CorruptedWorldException, NewerFormatException {
        int worldVersion = dataStream.readInt();

        byte[] chunkBytes = readCompressed(dataStream);
        Long2ObjectMap<SlimeChunk> chunks = readChunks(propertyMap, chunkBytes);

        byte[] extraTagBytes = readCompressed(dataStream);
        CompoundBinaryTag extraTag = readCompound(extraTagBytes);

        ConcurrentMap<String, BinaryTag> extraData = new ConcurrentHashMap<>();
        if (extraTag != null) extraTag.forEach(entry -> extraData.put(entry.getKey(), entry.getValue()));

        SlimePropertyMap worldPropertyMap = propertyMap;
        if (extraData.containsKey("properties")) {
            CompoundBinaryTag serializedSlimeProperties = (CompoundBinaryTag) extraData.get("properties");
            worldPropertyMap = SlimePropertyMap.fromCompound(serializedSlimeProperties);
            worldPropertyMap.merge(propertyMap);
        }

        return new com.infernalsuite.asp.skeleton.SkeletonSlimeWorld(worldName, loader, readOnly, chunks, extraData, worldPropertyMap, worldVersion);
    }

    private static Long2ObjectMap<SlimeChunk> readChunks(SlimePropertyMap slimePropertyMap, byte[] chunkBytes) throws IOException {
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

                // Block Light Nibble Array
                NibbleArray blockLightArray;
                if (chunkData.readBoolean()) {
                    byte[] blockLightByteArray = new byte[ARRAY_SIZE];
                    chunkData.read(blockLightByteArray);
                    blockLightArray = new NibbleArray(blockLightByteArray);
                } else {
                    blockLightArray = null;
                }

                // Sky Light Nibble Array
                NibbleArray skyLightArray;
                if (chunkData.readBoolean()) {
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

            // Tile Entities

            byte[] tileEntitiesRaw = read(chunkData);
            List<CompoundBinaryTag> tileEntities;
            CompoundBinaryTag tileEntitiesCompound = readCompound(tileEntitiesRaw);
            if (tileEntitiesCompound == null) {
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
            if (entitiesCompound == null) {
                entities = Collections.emptyList();
            } else {
                entities = entitiesCompound.getList("entities", BinaryTagTypes.COMPOUND).stream()
                        .map(tag -> (CompoundBinaryTag) tag)
                        .toList();
            }

            // Extra Tag
            byte[] rawExtra = read(chunkData);
            CompoundBinaryTag extra = readCompound(rawExtra);
            // If the extra tag is empty, the serializer will save it as null.
            // So if we deserialize a null extra tag, we will assume it was empty.
            if (extra == null) extra = CompoundBinaryTag.empty();

            chunkMap.put(Util.chunkPosition(x, z),
                    new com.infernalsuite.asp.skeleton.SlimeChunkSkeleton(x, z, chunkSections, heightMaps, tileEntities, entities, extra, null));
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

    private static CompoundBinaryTag readCompound(byte[] tagBytes) throws IOException {
        if (tagBytes.length == 0) return null;

        return BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(tagBytes));
    }
}
