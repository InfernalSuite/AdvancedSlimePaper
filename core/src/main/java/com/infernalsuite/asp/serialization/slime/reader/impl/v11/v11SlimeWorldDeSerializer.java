package com.infernalsuite.asp.serialization.slime.reader.impl.v11;

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
import com.infernalsuite.asp.skeleton.SlimeChunkSkeleton;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.adventure.nbt.*;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class v11SlimeWorldDeSerializer implements com.infernalsuite.asp.serialization.slime.reader.VersionedByteSlimeWorldReader<com.infernalsuite.asp.api.world.SlimeWorld> {

    public static final int ARRAY_SIZE = 16 * 16 * 16 / (8 / 4);

    @Override
    public SlimeWorld deserializeWorld(byte version, @Nullable SlimeLoader loader, String worldName, DataInputStream dataStream, SlimePropertyMap propertyMap, boolean readOnly) throws IOException, CorruptedWorldException, NewerFormatException {
        int worldVersion = dataStream.readInt();

        byte[] chunkBytes = readCompressed(dataStream);
        Long2ObjectMap<SlimeChunk> chunks = readChunks(propertyMap, chunkBytes);

        byte[] extraTagBytes = readCompressed(dataStream);
        CompoundBinaryTag extraTag = readCompound(extraTagBytes);

        SlimePropertyMap worldPropertyMap = propertyMap;
        CompoundBinaryTag propertiesMap = extraTag.get("properties") != null
                ? extraTag.getCompound("properties")
                : null;

        if (propertiesMap != null) {
            Map<String, BinaryTag> wpm = new HashMap<>();
            propertiesMap.forEach(entry -> wpm.put(entry.getKey(), entry.getValue()));

            worldPropertyMap = new SlimePropertyMap(wpm);
            worldPropertyMap.merge(propertyMap); // Override world properties
        }

        ConcurrentMap<String, BinaryTag> extraData = new ConcurrentHashMap<>();
        extraTag.forEach(entry -> extraData.put(entry.getKey(), entry.getValue()));

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

            int compressedTileEntitiesLength = chunkData.readInt();
            int decompressedTileEntitiesLength = chunkData.readInt();
            byte[] compressedTileEntitiesData = new byte[compressedTileEntitiesLength];
            byte[] decompressedTileEntitiesData = new byte[decompressedTileEntitiesLength];
            chunkData.read(compressedTileEntitiesData);
            Zstd.decompress(decompressedTileEntitiesData, compressedTileEntitiesData);

            CompoundBinaryTag tileEntitiesCompound = readCompound(decompressedTileEntitiesData);

            ListBinaryTag tileEntitiesTag = tileEntitiesCompound.getList("tileEntities", BinaryTagTypes.COMPOUND);
            List<CompoundBinaryTag> serializedTileEntities = new ArrayList<>(tileEntitiesTag.size());
            for (BinaryTag binaryTag : tileEntitiesTag) {
                serializedTileEntities.add((CompoundBinaryTag) binaryTag);
            }

            // Entities

            int compressedEntitiesLength = chunkData.readInt();
            int decompressedEntitiesLength = chunkData.readInt();
            byte[] compressedEntitiesData = new byte[compressedEntitiesLength];
            byte[] decompressedEntitiesData = new byte[decompressedEntitiesLength];
            chunkData.read(compressedEntitiesData);
            Zstd.decompress(decompressedEntitiesData, compressedEntitiesData);

            CompoundBinaryTag entitiesCompound = readCompound(decompressedEntitiesData);
            ListBinaryTag entitiesTag = entitiesCompound.getList("entities", BinaryTagTypes.COMPOUND);
            List<CompoundBinaryTag> serializedEntities = new ArrayList<>(entitiesTag.size());
            for (BinaryTag binaryTag : entitiesTag) {
                serializedEntities.add((CompoundBinaryTag) binaryTag);
            }
            
            chunkMap.put(Util.chunkPosition(x, z),
                    new SlimeChunkSkeleton(x, z, chunkSections, heightMaps, serializedTileEntities, serializedEntities, new HashMap<>(), null, null, null, null));
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

    private static CompoundBinaryTag readCompound(byte[] tagBytes) throws IOException {
        if (tagBytes.length == 0) return CompoundBinaryTag.empty();

        return BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(tagBytes));
    }
}
