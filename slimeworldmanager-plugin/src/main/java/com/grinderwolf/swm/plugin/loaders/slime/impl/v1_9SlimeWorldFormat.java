package com.grinderwolf.swm.plugin.loaders.slime.impl;

import com.flowpowered.nbt.*;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.github.luben.zstd.Zstd;
import com.grinderwolf.swm.api.exceptions.CorruptedWorldException;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeChunkSection;
import com.grinderwolf.swm.nms.NmsUtil;
import com.grinderwolf.swm.nms.world.SlimeLoadedWorld;
import com.grinderwolf.swm.plugin.SWMPlugin;
import com.grinderwolf.swm.plugin.loaders.slime.SlimeWorldReader;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Data;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;

public class v1_9SlimeWorldFormat implements SlimeWorldReader {

    @Override
    public SlimeLoadedWorld deserializeWorld(byte version, SlimeLoader loader, String worldName, DataInputStream dataStream, SlimePropertyMap propertyMap, boolean readOnly)
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
            Long2ObjectOpenHashMap<SlimeChunk> chunks = readChunks(worldVersion, version, worldName, minX, minZ, width, depth, chunkBitset, chunkData);

            // Entity deserialization
            CompoundTag entitiesCompound = readCompoundTag(entities);

            Long2ObjectOpenHashMap<List<CompoundTag>> entityStorage = new Long2ObjectOpenHashMap<>();
            if (entitiesCompound != null) {
                List<CompoundTag> serializedEntities = ((ListTag<CompoundTag>) entitiesCompound.getValue().get("entities")).getValue();

                for (CompoundTag entityCompound : serializedEntities) {
                    ListTag<DoubleTag> listTag = (ListTag<DoubleTag>) entityCompound.getAsListTag("Pos").get();

                    int chunkX = floor(listTag.getValue().get(0).getValue()) >> 4;
                    int chunkZ = floor(listTag.getValue().get(2).getValue()) >> 4;
                    long chunkKey = NmsUtil.asLong(chunkX, chunkZ);
                    SlimeChunk chunk = chunks.get(chunkKey);
                    if (chunk != null) {
                       chunk.getEntities().add(entityCompound);
                    }
                    if (entityStorage.containsKey(chunkKey)) {
                        entityStorage.get(chunkKey).add(entityCompound);
                    } else {
                        List<CompoundTag> entityStorageList = new ArrayList<>();
                        entityStorageList.add(entityCompound);
                        entityStorage.put(chunkKey, entityStorageList);
                    }
                }
            }

            // Tile Entity deserialization
            CompoundTag tileEntitiesCompound = readCompoundTag(tileEntities);

            if (tileEntitiesCompound != null) {
                ListTag<CompoundTag> tileEntitiesList = (ListTag<CompoundTag>) tileEntitiesCompound.getValue().get("tiles");
                for (CompoundTag tileEntityCompound : tileEntitiesList.getValue()) {
                    int chunkX = ((IntTag) tileEntityCompound.getValue().get("x")).getValue() >> 4;
                    int chunkZ = ((IntTag) tileEntityCompound.getValue().get("z")).getValue() >> 4;
                    long chunkKey = NmsUtil.asLong(chunkX, chunkZ);
                    SlimeChunk chunk = chunks.get(chunkKey);

                    if (chunk == null) {
                        throw new CorruptedWorldException(worldName);
                    }

                    chunk.getTileEntities().add(tileEntityCompound);
                }
            }

            // Extra Data
            CompoundTag extraCompound = readCompoundTag(extraTag);

            if (extraCompound == null) {
                extraCompound = new CompoundTag("", new CompoundMap());
            }

            // World Maps
            CompoundTag mapsCompound = readCompoundTag(mapsTag);
            List<CompoundTag> mapList;

            if (mapsCompound != null) {
                mapList = (List<CompoundTag>) mapsCompound.getAsListTag("maps").map(ListTag::getValue).orElse(new ArrayList<>());
            } else {
                mapList = new ArrayList<>();
            }


            // World properties
            SlimePropertyMap worldPropertyMap = propertyMap;
            Optional<CompoundMap> propertiesMap = extraCompound
                    .getAsCompoundTag("properties")
                    .map(CompoundTag::getValue);

            if (propertiesMap.isPresent()) {
                worldPropertyMap = new SlimePropertyMap(propertiesMap.get());
                worldPropertyMap.merge(propertyMap); // Override world properties
            } else if (propertyMap == null) { // Make sure the property map is never null
                worldPropertyMap = new SlimePropertyMap();
            }

            return SWMPlugin.getInstance().getNms().createSlimeWorld(loader, worldName, chunks, extraCompound, mapList, worldVersion, worldPropertyMap, readOnly, !readOnly, entityStorage);
        } catch (EOFException ex) {
            throw new CorruptedWorldException(worldName, ex);
        }
    }

    private static int floor(double num) {
        final int floor = (int) num;
        return floor == num ? floor : floor - (int) (Double.doubleToRawLongBits(num) >>> 63);
    }

    private static Long2ObjectOpenHashMap<SlimeChunk> readChunks(byte worldVersion, int version, String worldName, int minX, int minZ, int width, int depth, BitSet chunkBitset, byte[] chunkData) throws IOException {
        DataInputStream dataStream = new DataInputStream(new ByteArrayInputStream(chunkData));
        Long2ObjectOpenHashMap<SlimeChunk> chunkMap = new Long2ObjectOpenHashMap<>();

        for (int z = 0; z < depth; z++) {
            for (int x = 0; x < width; x++) {
                int bitsetIndex = z * width + x;

                if (chunkBitset.get(bitsetIndex)) {
                    // Height Maps
                    CompoundTag heightMaps;

                    if (worldVersion >= 0x04) {
                        int heightMapsLength = dataStream.readInt();
                        byte[] heightMapsArray = new byte[heightMapsLength];
                        dataStream.read(heightMapsArray);
                        heightMaps = readCompoundTag(heightMapsArray);

                        // Height Maps might be null if empty
                        if (heightMaps == null) {
                            heightMaps = new CompoundTag("", new CompoundMap());
                        }
                    } else {
                        int[] heightMap = new int[256];

                        for (int i = 0; i < 256; i++) {
                            heightMap[i] = dataStream.readInt();
                        }

                        CompoundMap map = new CompoundMap();
                        map.put("heightMap", new IntArrayTag("heightMap", heightMap));

                        heightMaps = new CompoundTag("", map);
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

                    chunkMap.put(NmsUtil.asLong(chunkX, chunkZ), new CraftSlimeChunk(worldName, chunkX, chunkZ,
                            data.sections, heightMaps, biomes, new ArrayList<>(), new ArrayList<>(), data.minSectionY, data.maxSectionY));
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

    @Data
    private static class ChunkSectionData {
        private final SlimeChunkSection[] sections;
        private final int minSectionY;
        private final int maxSectionY;
    }

    private static ChunkSectionData readChunkSectionsNew(DataInputStream dataStream, int worldVersion, int version) throws IOException {
        int minSectionY = dataStream.readInt();
        int maxSectionY = dataStream.readInt();
        int sectionCount = dataStream.readInt();
        SlimeChunkSection[] chunkSectionArray = new SlimeChunkSection[maxSectionY - minSectionY];

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
            CompoundTag blockStateTag = readCompoundTag(blockStateData);

            byte[] biomeData = new byte[dataStream.readInt()];
            dataStream.read(biomeData);
            CompoundTag biomeTag = readCompoundTag(biomeData);

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

            chunkSectionArray[y] = new CraftSlimeChunkSection(null, null, blockStateTag, biomeTag, blockLightArray, skyLightArray);
        }

        return new ChunkSectionData(chunkSectionArray, minSectionY, maxSectionY);
    }

    private static ChunkSectionData readChunkSections(DataInputStream dataStream, byte worldVersion, int version) throws IOException {
        SlimeChunkSection[] chunkSectionArray = new SlimeChunkSection[16];
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
                ListTag<CompoundTag> paletteTag = null;
                long[] blockStatesArray = null;

                // Palette
                int paletteLength = dataStream.readInt();
                List<CompoundTag> paletteList = new ArrayList<>(paletteLength);
                for (int index = 0; index < paletteLength; index++) {
                    int tagLength = dataStream.readInt();
                    byte[] serializedTag = new byte[tagLength];
                    dataStream.read(serializedTag);

                    CompoundTag tag = readCompoundTag(serializedTag);
                    paletteList.add(tag);
                }

                paletteTag = new ListTag<>("", TagType.TAG_COMPOUND, paletteList);

                // Block states
                int blockStatesArrayLength = dataStream.readInt();
                blockStatesArray = new long[blockStatesArrayLength];

                for (int index = 0; index < blockStatesArrayLength; index++) {
                    blockStatesArray[index] = dataStream.readLong();
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

                chunkSectionArray[i] = new CraftSlimeChunkSection(paletteTag, blockStatesArray, null, null, blockLightArray, skyLightArray);
            }
        }

        return new ChunkSectionData(chunkSectionArray, 0, 16);
    }

    private static CompoundTag readCompoundTag(byte[] serializedCompound) throws IOException {
        if (serializedCompound.length == 0) {
            return null;
        }

        NBTInputStream stream = new NBTInputStream(new ByteArrayInputStream(serializedCompound), NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);

        return (CompoundTag) stream.readTag();
    }
}
