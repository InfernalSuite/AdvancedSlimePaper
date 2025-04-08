package com.infernalsuite.asp.serialization.slime.reader.impl.v10;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.DoubleTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.stream.NBTInputStream;
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

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


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
        com.flowpowered.nbt.CompoundTag entitiesCompound = readCompound(entities);
        {
            List<CompoundTag> serializedEntities = ((ListTag<CompoundTag>) entitiesCompound.getValue().get("entities")).getValue();
            for (CompoundTag entityCompound : serializedEntities) {
                ListTag<DoubleTag> listTag = (ListTag<DoubleTag>) entityCompound.getAsListTag("Pos").get();

                int chunkX = listTag.getValue().get(0).getValue().intValue() >> 4;
                int chunkZ = listTag.getValue().get(2).getValue().intValue() >> 4;
                long chunkKey = Util.chunkPosition(chunkX, chunkZ);
                SlimeChunk chunk = chunks.get(chunkKey);
                if (chunk != null) {
                    chunk.getEntities().add(entityCompound);
                }
            }
        }

        // Tile Entity deserialization
        com.flowpowered.nbt.CompoundTag tileEntitiesCompound = readCompound(tileEntities);
        for (CompoundTag tileEntityCompound : ((com.flowpowered.nbt.ListTag<com.flowpowered.nbt.CompoundTag>) tileEntitiesCompound.getValue().get("tiles")).getValue()) {
            int chunkX = ((IntTag) tileEntityCompound.getValue().get("x")).getValue() >> 4;
            int chunkZ = ((IntTag) tileEntityCompound.getValue().get("z")).getValue() >> 4;
            long pos = Util.chunkPosition(chunkX, chunkZ);
            SlimeChunk chunk = chunks.get(pos);

            if (chunk == null) {
                throw new CorruptedWorldException(worldName);
            }

            chunk.getTileEntities().add(tileEntityCompound);
        }

        // Extra Data
        com.flowpowered.nbt.CompoundTag extraCompound = readCompound(extra);

        // World properties
        SlimePropertyMap worldPropertyMap = propertyMap;
        Optional<CompoundMap> propertiesMap = extraCompound
                .getAsCompoundTag("properties")
                .map(com.flowpowered.nbt.CompoundTag::getValue);

        if (propertiesMap.isPresent()) {
            worldPropertyMap = new SlimePropertyMap(propertiesMap.get());
            worldPropertyMap.merge(propertyMap); // Override world properties
        }

        return new com.infernalsuite.asp.skeleton.SkeletonSlimeWorld(worldName, loader, readOnly, chunks,
                extraCompound,
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
            com.flowpowered.nbt.CompoundTag heightMaps = readCompound(heightMapData);

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
                    com.flowpowered.nbt.CompoundTag blockStateTag = readCompound(blockStateData);

                    // Biome Data
                    byte[] biomeData = new byte[chunkData.readInt()];
                    chunkData.read(biomeData);
                    com.flowpowered.nbt.CompoundTag biomeTag = readCompound(biomeData);

                    chunkSectionArray[sectionId] = new com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton(
                            blockStateTag,
                            biomeTag,
                            blockLightArray,
                            skyLightArray);
                }

                chunkMap.put(Util.chunkPosition(x, z),
                        new com.infernalsuite.asp.skeleton.SlimeChunkSkeleton(x, z, chunkSectionArray, heightMaps, new ArrayList<>(), new ArrayList<>(), new CompoundTag("", new CompoundMap()), null)
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

    private static com.flowpowered.nbt.CompoundTag readCompound(byte[] bytes) throws IOException {
        if (bytes.length == 0) {
            return null;
        }

        NBTInputStream stream = new NBTInputStream(new ByteArrayInputStream(bytes), NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
        return (com.flowpowered.nbt.CompoundTag) stream.readTag();
    }


}