package com.infernalsuite.asp.serialization.slime.reader.impl.v1_9;

import com.github.luben.zstd.Zstd;
import com.infernalsuite.asp.SlimeLogger;
import com.infernalsuite.asp.Util;
import com.infernalsuite.asp.api.exceptions.CorruptedWorldException;
import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.utils.NibbleArray;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.adventure.nbt.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

class v1_9SlimeWorldDeserializer implements com.infernalsuite.asp.serialization.slime.reader.VersionedByteSlimeWorldReader<v1_9SlimeWorld> {

    @Override
    public v1_9SlimeWorld deserializeWorld(byte version, SlimeLoader loader, String worldName, DataInputStream dataStream, SlimePropertyMap propertyMap, boolean readOnly)
            throws IOException, CorruptedWorldException {

        try {

            // World version
            byte worldVersion;

            if (version >= 6) {
                worldVersion = dataStream.readByte();
            } else if (version >= 4) { // In v4 there's just a boolean indicating whether the world is pre-1.13 or post-1.13
                worldVersion = (byte) (dataStream.readBoolean() ? 0x04 : 0x01);
            } else {
                worldVersion = 0; // We'll try to automatically detect it later
            }

            // Chunk
            short minX = dataStream.readShort();
            short minZ = dataStream.readShort();
            int width = dataStream.readShort();
            int depth = dataStream.readShort();

            if (width <= 0 || depth <= 0) {
                throw new CorruptedWorldException(worldName);
            }

            int bitmaskSize = (int) Math.ceil((width * depth) / 8.0D);
            byte[] chunkBitmask = new byte[bitmaskSize];
            dataStream.read(chunkBitmask);
            BitSet chunkBitset = BitSet.valueOf(chunkBitmask);

            int compressedChunkDataLength = dataStream.readInt();
            int chunkDataLength = dataStream.readInt();
            byte[] compressedChunkData = new byte[compressedChunkDataLength];
            byte[] chunkData = new byte[chunkDataLength];

            dataStream.read(compressedChunkData);

            // Tile Entities
            int compressedTileEntitiesLength = dataStream.readInt();
            int tileEntitiesLength = dataStream.readInt();
            byte[] compressedTileEntities = new byte[compressedTileEntitiesLength];
            byte[] tileEntities = new byte[tileEntitiesLength];

            dataStream.read(compressedTileEntities);

            // Entities
            byte[] compressedEntities = new byte[0];
            byte[] entities = new byte[0];

            if (version >= 3) {
                boolean hasEntities = dataStream.readBoolean();

                if (hasEntities) {
                    int compressedEntitiesLength = dataStream.readInt();
                    int entitiesLength = dataStream.readInt();
                    compressedEntities = new byte[compressedEntitiesLength];
                    entities = new byte[entitiesLength];

                    dataStream.read(compressedEntities);
                }
            }

            // Extra NBT tag
            byte[] compressedExtraTag = new byte[0];
            byte[] extraTag = new byte[0];

            if (version >= 2) {
                int compressedExtraTagLength = dataStream.readInt();
                int extraTagLength = dataStream.readInt();
                compressedExtraTag = new byte[compressedExtraTagLength];
                extraTag = new byte[extraTagLength];

                dataStream.read(compressedExtraTag);
            }

            // World Map NBT tag
            byte[] compressedMapsTag = new byte[0];
            byte[] mapsTag = new byte[0];

            if (version >= 7) {
                int compressedMapsTagLength = dataStream.readInt();
                int mapsTagLength = dataStream.readInt();
                compressedMapsTag = new byte[compressedMapsTagLength];
                mapsTag = new byte[mapsTagLength];

                dataStream.read(compressedMapsTag);
            }

            if (dataStream.read() != -1) {
                throw new CorruptedWorldException(worldName);
            }

            // Data decompression
            Zstd.decompress(chunkData, compressedChunkData);
            Zstd.decompress(tileEntities, compressedTileEntities);
            Zstd.decompress(entities, compressedEntities);
            Zstd.decompress(extraTag, compressedExtraTag);
            Zstd.decompress(mapsTag, compressedMapsTag);

            // Chunk deserialization
            Long2ObjectMap<v1_9SlimeChunk> chunks = readChunks(worldVersion, version, worldName, minX, minZ, width, depth, chunkBitset, chunkData);

            // Entity deserialization
            CompoundBinaryTag entitiesCompound = readCompoundTag(entities);

            Long2ObjectMap<List<CompoundBinaryTag>> entityStorage = new Long2ObjectOpenHashMap<>();
            if (entitiesCompound != null) {
                ListBinaryTag serializedEntities = entitiesCompound.getList("entities", BinaryTagTypes.COMPOUND);

                SlimeLogger.debug("Serialized entities: " + serializedEntities);
                for (BinaryTag entityCompoundTag : serializedEntities) {
                    CompoundBinaryTag entityCompound = (CompoundBinaryTag) entityCompoundTag;

                    ListBinaryTag listTag = entityCompound.getList("Pos");

                    int chunkX = floor(listTag.getDouble(0)) >> 4;
                    int chunkZ = floor(listTag.getDouble(2)) >> 4;
                    long chunkKey = Util.chunkPosition(chunkX, chunkZ);
                    v1_9SlimeChunk chunk = chunks.get(chunkKey);
                    if (chunk != null) {
                        chunk.entities.add(entityCompound);
                    }
                    if (entityStorage.containsKey(chunkKey)) {
                        entityStorage.get(chunkKey).add(entityCompound);
                    } else {
                        List<CompoundBinaryTag> entityStorageList = new ArrayList<>();
                        entityStorageList.add(entityCompound);
                        entityStorage.put(chunkKey, entityStorageList);
                    }
                }
            }

            // Tile Entity deserialization
            CompoundBinaryTag tileEntitiesCompound = readCompoundTag(tileEntities);

            if (tileEntitiesCompound != null) {
                ListBinaryTag tileEntitiesList = tileEntitiesCompound.getList("tiles");
                for (BinaryTag tileEntityCompoundTag : tileEntitiesList) {
                    CompoundBinaryTag tileEntityCompound = (CompoundBinaryTag) tileEntityCompoundTag;
                    int chunkX = tileEntityCompound.getInt("x") >> 4;
                    int chunkZ = tileEntityCompound.getInt("z") >> 4;
                    v1_9SlimeChunk chunk = chunks.get(Util.chunkPosition(chunkX, chunkZ));

                    if (chunk == null) {
                        throw new CorruptedWorldException(worldName);
                    }

                    chunk.tileEntities.add(tileEntityCompound);
                }
            }

            // Extra Data
            CompoundBinaryTag extraCompound = readCompoundTag(extraTag);

            if (extraCompound == null) {
                extraCompound = CompoundBinaryTag.empty();
            }

            if (version <= 0x05) {}

            // World Maps
//            CompoundTag mapsCompound = readCompoundTag(mapsTag);
//            List<CompoundTag> mapList;
//
//            if (mapsCompound != null) {
//                mapList = (List<CompoundTag>) mapsCompound.getAsListTag("maps").map(ListTag::getValue).orElse(new ArrayList<>());
//            } else {
//                mapList = new ArrayList<>();
//            }


            // World properties
            SlimePropertyMap worldPropertyMap = propertyMap;
            CompoundBinaryTag propertiesMap = extraCompound
                    .getCompound("properties");

            if (!propertiesMap.isEmpty()) {
                Map<String, BinaryTag> wpm = new HashMap<>();
                propertiesMap.forEach(entry -> wpm.put(entry.getKey(), entry.getValue()));

                worldPropertyMap = new SlimePropertyMap(wpm);
                worldPropertyMap.merge(propertyMap); // Override world properties
            } else if (propertyMap == null) { // Make sure the property map is never null
                worldPropertyMap = new SlimePropertyMap();
            }


            ConcurrentMap<String, BinaryTag> extraData = new ConcurrentHashMap<>();
            extraCompound.forEach(entry -> extraData.put(entry.getKey(), entry.getValue()));

            return new v1_9SlimeWorld(
                    worldVersion,
                    worldName,
                    loader,
                    chunks,
                    extraData,
                    worldPropertyMap,
                    readOnly
            );
        } catch (EOFException ex) {
            throw new CorruptedWorldException(worldName, ex);
        }
    }

    private static int floor(double num) {
        final int floor = (int) num;
        return floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
    }

    private static Long2ObjectMap<v1_9SlimeChunk> readChunks(byte worldVersion, int version, String worldName, int minX, int minZ, int width, int depth, BitSet chunkBitset, byte[] chunkData) throws IOException {
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(chunkData));
        Long2ObjectMap<v1_9SlimeChunk> chunkMap = new Long2ObjectOpenHashMap<>();

        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                int bitsetIndex = z * width + x;

                if (chunkBitset.get(bitsetIndex)) {
                    // Height Maps
                    CompoundBinaryTag heightMaps;

                    if (worldVersion >= 0x04) {
                        int heightMapsLength = dataStream.readInt();
                        byte[] heightMapsArray = new byte[heightMapsLength];
                        dataStream.read(heightMapsArray);
                        heightMaps = readCompoundTag(heightMapsArray);

                        // Height Maps might be null if empty
                        if (heightMaps == null) {
                            heightMaps = CompoundBinaryTag.empty();
                        }
                    } else {
                        int[] heightMap = new int[256];

                        for (int i = 0; i < 256; i++) {
                            heightMap[i] = dataStream.readInt();
                        }

                        heightMaps = CompoundBinaryTag.builder()
                                .put("heightMap", IntArrayBinaryTag.intArrayBinaryTag(heightMap)).build();
                    }

                    // Biome array
                    int[] biomes = null;

                    if (version == 8 && worldVersion < 0x04) {
                        // Patch the v8 bug: biome array size is wrong for old worlds
                        dataStream.readInt();
                    }

                    if (worldVersion < 0x04) {
                        byte[] byteBiomes = new byte[256];
                        dataStream.read(byteBiomes);
                        biomes = toIntArray(byteBiomes);
                    } else if (worldVersion < 0x08) {
                        int biomesArrayLength = version >= 8 ? dataStream.readInt() : 256;
                        biomes = new int[biomesArrayLength];

                        for (int i = 0; i < biomes.length; i++) {
                            biomes[i] = dataStream.readInt();
                        }
                    }

                    // Chunk Sections
                    ChunkSectionData data = worldVersion < 0x08 ? readChunkSections(dataStream, worldVersion, version) : readChunkSectionsNew(dataStream, worldVersion, version);

                    int chunkX = minX + x;
                    int chunkZ = minZ + z;

                    chunkMap.put(Util.chunkPosition(chunkX, chunkZ), new v1_9SlimeChunk(
                            worldName,
                            chunkX,
                            chunkZ,
                            data.sections,
                            data.minSectionY,
                            data.maxSectionY,
                            heightMaps,
                            biomes,
                            new ArrayList<>(),
                            new ArrayList<>()
                    ));
                }
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

    private record ChunkSectionData(v1_9SlimeChunkSection[] sections, int minSectionY, int maxSectionY) {
    }

    private static ChunkSectionData readChunkSectionsNew(DataInputStream dataStream, int worldVersion, int version) throws IOException {
        int minSectionY = dataStream.readInt();
        int maxSectionY = dataStream.readInt();
        int sectionCount = dataStream.readInt();
        v1_9SlimeChunkSection[] chunkSectionArray = new v1_9SlimeChunkSection[maxSectionY - minSectionY];

        for (int i = 0; i < sectionCount; i++) {
            int y = dataStream.readInt();

            // Block Light Nibble Array
            NibbleArray blockLightArray;

            if (version < 5 || dataStream.readBoolean()) {
                byte[] blockLightByteArray = new byte[2048];
                dataStream.read(blockLightByteArray);
                blockLightArray = new NibbleArray((blockLightByteArray));
            } else {
                blockLightArray = null;
            }

            // Block data
            byte[] blockStateData = new byte[dataStream.readInt()];
            dataStream.read(blockStateData);
            CompoundBinaryTag blockStateTag = readCompoundTag(blockStateData);

            byte[] biomeData = new byte[dataStream.readInt()];
            dataStream.read(biomeData);
            CompoundBinaryTag biomeTag = readCompoundTag(biomeData);

            // Sky Light Nibble Array
            NibbleArray skyLightArray;

            if (version < 5 || dataStream.readBoolean()) {
                byte[] skyLightByteArray = new byte[2048];
                dataStream.read(skyLightByteArray);
                skyLightArray = new NibbleArray((skyLightByteArray));
            } else {
                skyLightArray = null;
            }

            // HypixelBlocks 3
            if (version < 4) {
                short hypixelBlocksLength = dataStream.readShort();
                dataStream.skip(hypixelBlocksLength);
            }

            chunkSectionArray[y] = new v1_9SlimeChunkSection(null, null, null, null, blockStateTag, biomeTag, blockLightArray, skyLightArray);
        }

        return new ChunkSectionData(chunkSectionArray, minSectionY, maxSectionY);
    }

    private static ChunkSectionData readChunkSections(DataInputStream dataStream, byte worldVersion, int version) throws IOException {
        v1_9SlimeChunkSection[] chunkSectionArray = new v1_9SlimeChunkSection[16];
        byte[] sectionBitmask = new byte[2];
        dataStream.read(sectionBitmask);
        BitSet sectionBitset = BitSet.valueOf(sectionBitmask);

        for (int i = 0; i < 16; i++) {
            if (sectionBitset.get(i)) {
                // Block Light Nibble Array
                NibbleArray blockLightArray;

                if (version < 5 || dataStream.readBoolean()) {
                    byte[] blockLightByteArray = new byte[2048];
                    dataStream.read(blockLightByteArray);
                    blockLightArray = new NibbleArray((blockLightByteArray));
                } else {
                    blockLightArray = null;
                }

                // Block data
                byte[] blockArray;
                NibbleArray dataArray;

                ListBinaryTag paletteTag;
                long[] blockStatesArray;

                if (worldVersion >= 0x04) {
                    // Post 1.13
                    // Palette
                    int paletteLength = dataStream.readInt();
                    List<CompoundBinaryTag> paletteList = new ArrayList<>(paletteLength);
                    for (int index = 0; index < paletteLength; index++) {
                        int tagLength = dataStream.readInt();
                        byte[] serializedTag = new byte[tagLength];
                        dataStream.read(serializedTag);

                        CompoundBinaryTag tag = readCompoundTag(serializedTag);
                        paletteList.add(tag);
                    }

                    paletteTag = ListBinaryTag.builder()
                            .add(paletteList)
                            .build();

                    // Block states
                    int blockStatesArrayLength = dataStream.readInt();
                    blockStatesArray = new long[blockStatesArrayLength];

                    for (int index = 0; index < blockStatesArrayLength; index++) {
                        blockStatesArray[index] = dataStream.readLong();
                    }

                    blockArray = null;
                    dataArray = null;
                } else {
                    // Pre 1.13
                    blockArray = new byte[4096];
                    dataStream.read(blockArray);

                    // Block Data Nibble Array
                    byte[] dataByteArray = new byte[2048];
                    dataStream.read(dataByteArray);
                    dataArray = new NibbleArray((dataByteArray));

                    paletteTag = null;
                    blockStatesArray = null;
                }

                // Sky Light Nibble Array
                NibbleArray skyLightArray;

                if (version < 5 || dataStream.readBoolean()) {
                    byte[] skyLightByteArray = new byte[2048];
                    dataStream.read(skyLightByteArray);
                    skyLightArray = new NibbleArray((skyLightByteArray));
                } else {
                    skyLightArray = null;
                }

                // HypixelBlocks 3
                if (version < 4) {
                    short hypixelBlocksLength = dataStream.readShort();
                    dataStream.skip(hypixelBlocksLength);
                }

                chunkSectionArray[i] = new v1_9SlimeChunkSection(blockArray, dataArray, paletteTag, blockStatesArray, null, null, blockLightArray, skyLightArray);
            }
        }

        return new ChunkSectionData(chunkSectionArray, 0, 16);
    }

    private static CompoundBinaryTag readCompoundTag(byte[] serializedCompound) throws IOException {
        if (serializedCompound.length == 0) {
            return null;
        }

        return BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(serializedCompound));
    }
}
