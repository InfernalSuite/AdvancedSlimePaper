package com.infernalsuite.asp.serialization.slime.reader.impl.v11;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.stream.NBTInputStream;
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
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Optional;

public class v11SlimeWorldDeSerializer implements com.infernalsuite.asp.serialization.slime.reader.VersionedByteSlimeWorldReader<com.infernalsuite.asp.api.world.SlimeWorld> {

    public static final int ARRAY_SIZE = 16 * 16 * 16 / (8 / 4);

    @Override
    public SlimeWorld deserializeWorld(byte version, @Nullable SlimeLoader loader, String worldName, DataInputStream dataStream, SlimePropertyMap propertyMap, boolean readOnly) throws IOException, CorruptedWorldException, NewerFormatException {
        int worldVersion = dataStream.readInt();

        byte[] chunkBytes = readCompressed(dataStream);
        Long2ObjectMap<SlimeChunk> chunks = readChunks(propertyMap, chunkBytes);

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

        return new com.infernalsuite.asp.skeleton.SkeletonSlimeWorld(worldName, loader, readOnly, chunks, extraTag, worldPropertyMap, worldVersion);
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
                CompoundTag blockStateTag = readCompound(blockStateData);

                // Biome Data
                byte[] biomeData = new byte[chunkData.readInt()];
                chunkData.read(biomeData);
                CompoundTag biomeTag = readCompound(biomeData);

                chunkSections[sectionId] = new com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton(blockStateTag, biomeTag, blockLightArray, skyLightArray);
            }

            // HeightMaps
            byte[] heightMapData = new byte[chunkData.readInt()];
            chunkData.read(heightMapData);
            CompoundTag heightMaps = readCompound(heightMapData);

            // Tile Entities

            int compressedTileEntitiesLength = chunkData.readInt();
            int decompressedTileEntitiesLength = chunkData.readInt();
            byte[] compressedTileEntitiesData = new byte[compressedTileEntitiesLength];
            byte[] decompressedTileEntitiesData = new byte[decompressedTileEntitiesLength];
            chunkData.read(compressedTileEntitiesData);
            Zstd.decompress(decompressedTileEntitiesData, compressedTileEntitiesData);

            CompoundTag tileEntitiesCompound = readCompound(decompressedTileEntitiesData);
            @SuppressWarnings("unchecked")
            List<CompoundTag> serializedTileEntities = ((ListTag<CompoundTag>) tileEntitiesCompound.getValue().get("tileEntities")).getValue();

            // Entities

            int compressedEntitiesLength = chunkData.readInt();
            int decompressedEntitiesLength = chunkData.readInt();
            byte[] compressedEntitiesData = new byte[compressedEntitiesLength];
            byte[] decompressedEntitiesData = new byte[decompressedEntitiesLength];
            chunkData.read(compressedEntitiesData);
            Zstd.decompress(decompressedEntitiesData, compressedEntitiesData);

            CompoundTag entitiesCompound = readCompound(decompressedEntitiesData);
            @SuppressWarnings("unchecked")
            List<CompoundTag> serializedEntities = ((ListTag<CompoundTag>) entitiesCompound.getValue().get("entities")).getValue();

            chunkMap.put(Util.chunkPosition(x, z),
                    new com.infernalsuite.asp.skeleton.SlimeChunkSkeleton(x, z, chunkSections, heightMaps, serializedTileEntities, serializedEntities, new CompoundTag("", new CompoundMap()), null));
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

    private static CompoundTag readCompound(byte[] tagBytes) throws IOException {
        if (tagBytes.length == 0) return null;

        NBTInputStream nbtStream = new NBTInputStream(new ByteArrayInputStream(tagBytes), NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
        return (CompoundTag) nbtStream.readTag();
    }
}
