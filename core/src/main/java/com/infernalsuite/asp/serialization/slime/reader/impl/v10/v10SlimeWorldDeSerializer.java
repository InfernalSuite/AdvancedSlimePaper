package com.infernalsuite.asp.serialization.slime.reader.impl.v10;

import com.github.luben.zstd.Zstd;
import com.infernalsuite.asp.Util;
import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.serialization.slime.reader.VersionedByteSlimeWorldReader;
import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;

import com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.asp.skeleton.SlimeChunkSkeleton;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.adventure.nbt.*;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


class v10SlimeWorldDeSerializer implements VersionedByteSlimeWorldReader<SlimeWorld> {

    public static final int ARRAY_SIZE = 16 * 16 * 16 / (8 / 4); // blocks / bytes per block

    @SuppressWarnings("unchecked")
    @Override
    public SlimeWorld deserializeWorld(byte version, SlimeLoader loader, String worldName, DataInputStream dataStream, SlimePropertyMap propertyMap, boolean readOnly)
            throws IOException, CorruptedWorldException {

        // World version
        int worldVersion = dataStream.readInt();
        // Chunk Data

        byte[] chunkBytes = readCompressed(dataStream);
        Long2ObjectMap<SlimeChunk> chunks = readChunks(propertyMap, chunkBytes);

        byte[] tileEntities = readCompressed(dataStream);
        byte[] entities = readCompressed(dataStream);
        byte[] extra = readCompressed(dataStream);

        // Entity deserialization
        CompoundBinaryTag entitiesCompound = readCompound(entities);
        if(!entitiesCompound.isEmpty()) {
            for (BinaryTag binaryTag : entitiesCompound.getList("entities", BinaryTagTypes.COMPOUND)) {
                CompoundBinaryTag entityCompound = (CompoundBinaryTag) binaryTag;

                ListBinaryTag listTag = entityCompound.getList("Pos", BinaryTagTypes.DOUBLE);

                int chunkX = ((int) listTag.getDouble(0)) >> 4;
                int chunkZ = ((int) listTag.getDouble(2)) >> 4;
                long chunkKey = Util.chunkPosition(chunkX, chunkZ);
                SlimeChunk chunk = chunks.get(chunkKey);
                if (chunk != null) {
                    chunk.getEntities().add(entityCompound);
                }
            }
        }

        // Tile Entity deserialization
        CompoundBinaryTag tileEntitiesCompound = readCompound(tileEntities);
        if(!tileEntitiesCompound.isEmpty()) {
            for (BinaryTag binaryTag : (tileEntitiesCompound.getList("tiles", BinaryTagTypes.COMPOUND))) {
                CompoundBinaryTag tileEntityCompound = (CompoundBinaryTag) binaryTag;

                int chunkX = tileEntityCompound.getInt("x") >> 4;
                int chunkZ = tileEntityCompound.getInt("z") >> 4;
                long pos = Util.chunkPosition(chunkX, chunkZ);
                SlimeChunk chunk = chunks.get(pos);

                if (chunk == null) {
                    throw new CorruptedWorldException(worldName);
                }

                chunk.getTileEntities().add(tileEntityCompound);
            }
        }

        // Extra Data
        CompoundBinaryTag extraCompound = readCompound(extra);

        // World properties
        SlimePropertyMap worldPropertyMap = propertyMap;
        CompoundBinaryTag propertiesMap = extraCompound != null && extraCompound.get("properties") != null
                ? extraCompound.getCompound("properties")
                : null;

        if (propertiesMap != null) {
            Map<String, BinaryTag> wpm = new HashMap<>();
            propertiesMap.forEach(entry -> wpm.put(entry.getKey(), entry.getValue()));

            worldPropertyMap = new SlimePropertyMap(wpm);
            worldPropertyMap.merge(propertyMap); // Override world properties
        }

        ConcurrentMap<String, BinaryTag> extraData = new ConcurrentHashMap<>();
        extraCompound.forEach(entry -> extraData.put(entry.getKey(), entry.getValue()));

        return new com.infernalsuite.asp.skeleton.SkeletonSlimeWorld(worldName, loader, readOnly, chunks,
                extraData,
                worldPropertyMap,
                worldVersion
        );
    }

    private static Long2ObjectMap<SlimeChunk> readChunks(SlimePropertyMap slimePropertyMap, byte[] bytes) throws IOException {
        Long2ObjectMap<SlimeChunk> chunkMap = new Long2ObjectOpenHashMap<>();
        DataInputStream chunkData = new DataInputStream(new ByteArrayInputStream(bytes));

        int chunks = chunkData.readInt();
        for (int i = 0; i < chunks; i++) {
            // coords
            int x = chunkData.readInt();
            int z = chunkData.readInt();

            // Height Maps
            byte[] heightMapData = new byte[chunkData.readInt()];
            chunkData.read(heightMapData);
            CompoundBinaryTag heightMaps = readCompound(heightMapData);

            // Chunk Sections
            {
                // See WorldUtils
                int sectionAmount = slimePropertyMap.getValue(SlimeProperties.CHUNK_SECTION_MAX) - slimePropertyMap.getValue(SlimeProperties.CHUNK_SECTION_MIN) + 1;
                SlimeChunkSection[] chunkSectionArray = new SlimeChunkSection[sectionAmount];

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

                    // Block data
                    byte[] blockStateData = new byte[chunkData.readInt()];
                    chunkData.read(blockStateData);
                    CompoundBinaryTag blockStateTag = readCompound(blockStateData);

                    // Biome Data
                    byte[] biomeData = new byte[chunkData.readInt()];
                    chunkData.read(biomeData);
                    CompoundBinaryTag biomeTag = readCompound(biomeData);

                    chunkSectionArray[sectionId] = new SlimeChunkSectionSkeleton(
                            blockStateTag,
                            biomeTag,
                            blockLightArray,
                            skyLightArray
                    );
                }

                chunkMap.put(Util.chunkPosition(x, z),
                        new SlimeChunkSkeleton(x, z, chunkSectionArray, heightMaps, new ArrayList<>(), new ArrayList<>(), new HashMap<>(), null, null, null, null)
                );
            }
        }

        return chunkMap;
    }

    private static int[] toIntArray(byte[] buf) {
        ByteBuffer buffer = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN);
        int[] ret = new int[buf.length / 4];

        buffer.asIntBuffer().get(ret);

        return ret;
    }

    private static byte[] readCompressed(DataInputStream stream) throws IOException {
        int compressedLength = stream.readInt();
        int normalLength = stream.readInt();
        byte[] compressed = new byte[compressedLength];
        byte[] normal = new byte[normalLength];

        stream.read(compressed);
        Zstd.decompress(normal, compressed);
        return normal;
    }

    private static @NotNull CompoundBinaryTag readCompound(byte[] tagBytes) throws IOException {
        if (tagBytes.length == 0) return CompoundBinaryTag.empty();

        return BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(tagBytes));
    }

}