package com.infernalsuite.asp.serialization.slime;

import com.github.luben.zstd.Zstd;
import com.infernalsuite.asp.api.utils.SlimeFormat;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SlimeSerializer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SlimeSerializer.class);

    public static byte[] serialize(SlimeWorld world) {
        Map<String, BinaryTag> extraData = world.getExtraData();
        SlimePropertyMap propertyMap = world.getPropertyMap();

        // Store world properties
        if (!extraData.containsKey("properties")) {
            extraData.putIfAbsent("properties", propertyMap.toCompound());
        } else {
            extraData.replace("properties", propertyMap.toCompound());
        }

        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        try {
            // File Header and Slime version
            outStream.write(SlimeFormat.SLIME_HEADER);
            outStream.writeByte(SlimeFormat.SLIME_VERSION);

            // World version
            outStream.writeInt(world.getDataVersion());

            // Chunks
            byte[] chunkData = serializeChunks(world, world.getChunkStorage());
            byte[] compressedChunkData = Zstd.compress(chunkData);

            outStream.writeInt(compressedChunkData.length);
            outStream.writeInt(chunkData.length);
            outStream.write(compressedChunkData);
            
            // Extra Tag
            byte[] extra = serializeCompoundTag(CompoundBinaryTag.builder().put(extraData).build());
            byte[] compressedExtra = Zstd.compress(extra);

            outStream.writeInt(compressedExtra.length);
            outStream.writeInt(extra.length);
            outStream.write(compressedExtra);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return outByteStream.toByteArray();
    }

    static byte[] serializeChunks(SlimeWorld world, Collection<SlimeChunk> chunks) throws IOException {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        // Prune chunks
        List<SlimeChunk> chunksToSave = chunks.stream()
                .filter(chunk -> !ChunkPruner.canBePruned(world, chunk))
                .toList();

        outStream.writeInt(chunksToSave.size());
        for (SlimeChunk chunk : chunksToSave) {
            outStream.writeInt(chunk.getX());
            outStream.writeInt(chunk.getZ());

            // Chunk sections
            SlimeChunkSection[] sections = Arrays.stream(chunk.getSections()).filter(Objects::nonNull).toList().toArray(new SlimeChunkSection[0]);

            outStream.writeInt(sections.length);
            for (SlimeChunkSection slimeChunkSection : sections) {
                // Block Light
                boolean hasBlockLight = slimeChunkSection.getBlockLight() != null;
                outStream.writeBoolean(hasBlockLight);

                if (hasBlockLight) {
                    outStream.write(slimeChunkSection.getBlockLight().getBacking());
                }

                // Sky Light
                boolean hasSkyLight = slimeChunkSection.getSkyLight() != null;
                outStream.writeBoolean(hasSkyLight);

                if (hasSkyLight) {
                    outStream.write(slimeChunkSection.getSkyLight().getBacking());
                }

                // Block Data
                byte[] serializedBlockStates = serializeCompoundTag(slimeChunkSection.getBlockStatesTag());
                outStream.writeInt(serializedBlockStates.length);
                outStream.write(serializedBlockStates);

                byte[] serializedBiomes = serializeCompoundTag(slimeChunkSection.getBiomeTag());
                outStream.writeInt(serializedBiomes.length);
                outStream.write(serializedBiomes);
            }

            // Height Maps
            byte[] heightMaps = serializeCompoundTag(chunk.getHeightMaps());
            outStream.writeInt(heightMaps.length);
            outStream.write(heightMaps);

            // Tile entities
            ListBinaryTag tileEntitiesNbtList = ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, yayGenerics(chunk.getTileEntities()));
            CompoundBinaryTag tileEntitiesCompound = CompoundBinaryTag.builder().put("tileEntities", tileEntitiesNbtList).build();
            byte[] tileEntitiesData = serializeCompoundTag(tileEntitiesCompound);

            outStream.writeInt(tileEntitiesData.length);
            outStream.write(tileEntitiesData);

            // Entities
            ListBinaryTag entitiesNbtList = ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, yayGenerics(chunk.getEntities()));
            CompoundBinaryTag entitiesCompound = CompoundBinaryTag.builder().put("entities", entitiesNbtList).build();
            byte[] entitiesData = serializeCompoundTag(entitiesCompound);

            outStream.writeInt(entitiesData.length);
            outStream.write(entitiesData);

            // Extra Tag
            if (chunk.getExtraData() == null) {
                LOGGER.warn("Chunk at " + chunk.getX() + ", " + chunk.getZ() + " from world " + world.getName() + " has no extra data! When deserialized, this chunk will have an empty extra data tag!");
            }
            byte[] extra = serializeCompoundTag(CompoundBinaryTag.from(chunk.getExtraData()));

            outStream.writeInt(extra.length);
            outStream.write(extra);
        }

        return outByteStream.toByteArray();
    }

    protected static byte[] serializeCompoundTag(CompoundBinaryTag tag) throws IOException {
        if (tag == null || tag.size() == 0) return new byte[0];

        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        BinaryTagIO.writer().write(tag, outByteStream);

        return outByteStream.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static List<BinaryTag> yayGenerics(final List<? extends BinaryTag> tags) {
        return (List<BinaryTag>) tags;
    }

}
