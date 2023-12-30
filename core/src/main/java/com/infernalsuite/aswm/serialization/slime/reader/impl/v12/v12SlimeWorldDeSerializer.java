package com.infernalsuite.aswm.serialization.slime.reader.impl.v12;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.github.luben.zstd.Zstd;
import com.infernalsuite.aswm.ChunkPos;
import com.infernalsuite.aswm.api.exceptions.CorruptedWorldException;
import com.infernalsuite.aswm.api.exceptions.NewerFormatException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import com.infernalsuite.aswm.api.utils.NibbleArray;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.serialization.slime.reader.VersionedByteSlimeWorldReader;
import com.infernalsuite.aswm.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.aswm.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.aswm.skeleton.SlimeChunkSkeleton;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class v12SlimeWorldDeSerializer implements VersionedByteSlimeWorldReader<SlimeWorld> {

    public static final int ARRAY_SIZE = 16 * 16 * 16 / (8 / 4);

    @Override
    public SlimeWorld deserializeWorld(byte version, @Nullable SlimeLoader loader, String worldName, DataInputStream dataStream, SlimePropertyMap propertyMap, boolean readOnly) throws IOException, CorruptedWorldException, NewerFormatException {
        int worldVersion = dataStream.readInt();

        byte[] chunkBytes = readCompressed(dataStream);
        Map<ChunkPos, SlimeChunk> chunks = readChunks(propertyMap, chunkBytes);

        byte[] extraTagBytes = readCompressed(dataStream);
        CompoundTag extraTag = readCompound(extraTagBytes);

        SlimePropertyMap worldPropertyMap = propertyMap;
        Optional<CompoundMap> propertiesMap = extraTag
                .getAsCompoundTag("properties")
                .map(CompoundTag::getValue);

        if (propertiesMap.isPresent()) {
            worldPropertyMap = new SlimePropertyMap(propertiesMap.get());
            worldPropertyMap.merge(propertyMap);
        }

        return new SkeletonSlimeWorld(worldName, loader, readOnly, chunks, extraTag, worldPropertyMap, worldVersion);
    }

    private static Map<ChunkPos, SlimeChunk> readChunks(SlimePropertyMap slimePropertyMap, byte[] chunkBytes) throws IOException {
        Map<ChunkPos, SlimeChunk> chunkMap = new HashMap<>();
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
                CompoundTag blockStateTag = readCompound(blockStateData);

                // Biome Data
                byte[] biomeData = new byte[chunkData.readInt()];
                chunkData.read(biomeData);
                CompoundTag biomeTag = readCompound(biomeData);

                chunkSections[sectionId] = new SlimeChunkSectionSkeleton(blockStateTag, biomeTag, blockLightArray, skyLightArray);
            }

            // HeightMaps
            byte[] heightMapData = new byte[chunkData.readInt()];
            chunkData.read(heightMapData);
            CompoundTag heightMaps = readCompound(heightMapData);

            // Tile Entities

            byte[] tileEntities = read(chunkData);

            CompoundTag tileEntitiesCompound = readCompound(tileEntities);
            @SuppressWarnings("unchecked")
            List<CompoundTag> serializedTileEntities = ((ListTag<CompoundTag>) tileEntitiesCompound.getValue().get("tileEntities")).getValue();

            // Entities

            byte[] entities = read(chunkData);

            CompoundTag entitiesCompound = readCompound(entities);
            @SuppressWarnings("unchecked")
            List<CompoundTag> serializedEntities = ((ListTag<CompoundTag>) entitiesCompound.getValue().get("entities")).getValue();


            // Extra Tag
            byte[] rawExtra = read(chunkData);
            CompoundTag extra = readCompound(rawExtra);
            // If the extra tag is empty, the serializer will save it as null.
            // So if we deserialize a null extra tag, we will assume it was empty.
            if (extra == null) {
                extra = new CompoundTag("", new CompoundMap());
            }

            chunkMap.put(new ChunkPos(x, z),
                    new SlimeChunkSkeleton(x, z, chunkSections, heightMaps, serializedTileEntities, serializedEntities, extra));
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

    private static CompoundTag readCompound(byte[] tagBytes) throws IOException {
        if (tagBytes.length == 0) {
            return null;
        }

        NBTInputStream nbtStream = new NBTInputStream(new ByteArrayInputStream(tagBytes), NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
        return (CompoundTag) nbtStream.readTag();
    }
}
