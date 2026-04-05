package com.infernalsuite.asp.serialization.slime.reader.impl.v11;

import com.github.luben.zstd.ZstdInputStream;
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
import com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.asp.skeleton.SlimeChunkSkeleton;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.adventure.nbt.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.DataInput;
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

        DataInputStream chunkBytes = openCompressedStream(dataStream);
        Long2ObjectMap<SlimeChunk> chunks = readChunks(propertyMap, chunkBytes);

        CompoundBinaryTag extraTag = readCompressedCompound(dataStream);

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

    private static Long2ObjectMap<SlimeChunk> readChunks(SlimePropertyMap slimePropertyMap, DataInputStream chunkData) throws IOException {
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
                CompoundBinaryTag blockStateTag = readLimitedCompound(chunkData);

                // Biome Data
                CompoundBinaryTag biomeTag = readLimitedCompound(chunkData);

                chunkSections[sectionId] = new SlimeChunkSectionSkeleton(blockStateTag, biomeTag, blockLightArray, skyLightArray);
            }

            // HeightMaps
            CompoundBinaryTag heightMaps = readLimitedCompound(chunkData);

            // Tile Entities

            CompoundBinaryTag tileEntitiesCompound = readCompressedCompound(chunkData);

            ListBinaryTag tileEntitiesTag = tileEntitiesCompound.getList("tileEntities", BinaryTagTypes.COMPOUND);
            List<CompoundBinaryTag> serializedTileEntities = new ArrayList<>(tileEntitiesTag.size());
            for (BinaryTag binaryTag : tileEntitiesTag) {
                serializedTileEntities.add((CompoundBinaryTag) binaryTag);
            }

            // Entities

            CompoundBinaryTag entitiesCompound = readCompressedCompound(chunkData);
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

    private static DataInputStream openCompressedStream(DataInputStream stream) throws IOException {
        int compressedLength = stream.readInt();
        stream.readInt(); //Decompressed length, legacy

        LimitedInputStream limitedInputStream = new LimitedInputStream(stream, compressedLength);
        ZstdInputStream inputStream = new ZstdInputStream(limitedInputStream);
        return new DataInputStream(inputStream);
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
        ZstdInputStream zstd = new ZstdInputStream(limitedInputStream);

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
