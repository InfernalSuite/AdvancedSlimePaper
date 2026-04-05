package com.infernalsuite.asp.serialization.slime;

import com.github.luben.zstd.ZstdOutputStream;
import com.infernalsuite.asp.api.utils.SlimeFormat;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import com.infernalsuite.asp.serialization.slime.reader.impl.v13.v13AdditionalWorldData;
import com.infernalsuite.asp.util.CountingOutputStream;
import com.infernalsuite.asp.util.ThrowingConsumer;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;

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

            EnumSet<v13AdditionalWorldData> additionalWorldData = EnumSet.noneOf(v13AdditionalWorldData.class);
            if (world.getPropertyMap().getValue(SlimeProperties.SAVE_POI)) {
                additionalWorldData.add(v13AdditionalWorldData.POI_CHUNKS);
            }
            if (world.getPropertyMap().getValue(SlimeProperties.SAVE_BLOCK_TICKS)) {
                additionalWorldData.add(v13AdditionalWorldData.BLOCK_TICKS);
            }
            if (world.getPropertyMap().getValue(SlimeProperties.SAVE_FLUID_TICKS)) {
                additionalWorldData.add(v13AdditionalWorldData.FLUID_TICKS);
            }
            outStream.writeByte(v13AdditionalWorldData.fromSet(additionalWorldData));

            // Chunks

            writeCompressed(outStream, value -> serializeChunks(value, world, world.getChunkStorage(), additionalWorldData));

            writeCompressed(outStream, value -> {
                //Avoid a buffered output stream by casting to DataOutput. Buffered Output Streams make the memory usage explode
                BinaryTagIO.writer().write(CompoundBinaryTag.builder().put(extraData).build(), (DataOutput) new DataOutputStream(value));
            });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return outByteStream.toByteArray();
    }

    static void serializeChunks(DataOutputStream outStream, SlimeWorld world, Collection<SlimeChunk> chunks, EnumSet<v13AdditionalWorldData> data) throws IOException {

        // Prune chunks
        List<SlimeChunk> chunksToSave = chunks.stream()
                .filter(chunk -> !ChunkPruner.canBePruned(world, chunk))
                .toList();

        outStream.writeInt(chunksToSave.size());
        for (SlimeChunk chunk : chunksToSave) {
            outStream.writeInt(chunk.getX());
            outStream.writeInt(chunk.getZ());

            // Chunk sections
            SlimeChunkSection[] sections = chunk.getSections();

            outStream.writeInt(sections.length);
            for (SlimeChunkSection slimeChunkSection : sections) {
                byte sectionFlags = 0;

                boolean hasBlockLight = slimeChunkSection.getBlockLight() != null;
                boolean hasSkyLight = slimeChunkSection.getSkyLight() != null;

                if(hasBlockLight) {
                    sectionFlags = (byte) (sectionFlags | 1);
                }
                if(hasSkyLight) {
                    sectionFlags = (byte) (sectionFlags | (1 << 1));
                }
                outStream.write(sectionFlags);

                // Block Light
                if (hasBlockLight) {
                    outStream.write(slimeChunkSection.getBlockLight().getBacking());
                }

                // Sky Light
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

            if (data.contains(v13AdditionalWorldData.POI_CHUNKS)) {
                byte[] poiData = serializeCompoundTag(chunk.getPoiChunkSections());
                outStream.writeInt(poiData.length);
                outStream.write(poiData);
            }

            if (data.contains(v13AdditionalWorldData.BLOCK_TICKS)) {
                byte[] blockTicksData = serializeCompoundTag(wrap("block_ticks", chunk.getBlockTicks()));
                outStream.writeInt(blockTicksData.length);
                outStream.write(blockTicksData);
            }

            if (data.contains(v13AdditionalWorldData.FLUID_TICKS)) {
                byte[] fluidTicksData = serializeCompoundTag(wrap("fluid_ticks", chunk.getFluidTicks()));
                outStream.writeInt(fluidTicksData.length);
                outStream.write(fluidTicksData);
            }

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
                LOGGER.warn("Chunk at {}, {} from world {} has no extra data! When deserialized, this chunk will have an empty extra data tag!", chunk.getX(), chunk.getZ(), world.getName());
            }
            byte[] extra = serializeCompoundTag(CompoundBinaryTag.from(chunk.getExtraData()));

            outStream.writeInt(extra.length);
            outStream.write(extra);
        }
    }

    private static void writeCompressed(DataOutputStream out, ThrowingConsumer<DataOutputStream> writer) throws Exception {
        ByteArrayOutputStream compressedOut = new ByteArrayOutputStream();
        ZstdOutputStream zstd = new ZstdOutputStream(compressedOut);
        DataOutputStream dataOut = new DataOutputStream(zstd);

        CountingOutputStream counting = new CountingOutputStream(dataOut);

        // write uncompressed data into zstd stream
        writer.accept(new DataOutputStream(counting));

        dataOut.flush();
        zstd.close();

        byte[] compressed = compressedOut.toByteArray();

        out.writeInt(compressed.length);
        out.writeInt((int) counting.getCount());
        out.write(compressed);
    }

    private static CompoundBinaryTag wrap(String key, ListBinaryTag list) {
        if(list == null || list.isEmpty()) {
            return null;
        }
        return CompoundBinaryTag.builder().put(key, list).build();
    }

    protected static byte[] serializeCompoundTag(CompoundBinaryTag tag) throws IOException {
        if (tag == null || tag.isEmpty()) return new byte[0];

        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        //Avoid a buffered output stream by casting to DataOutput. Buffered Output Streams make the memory usage explode
        BinaryTagIO.writer().write(tag, (DataOutput) new DataOutputStream(outByteStream));

        return outByteStream.toByteArray();
    }

    @SuppressWarnings("unchecked")
    private static List<BinaryTag> yayGenerics(final List<? extends BinaryTag> tags) {
        return (List<BinaryTag>) tags;
    }

}
