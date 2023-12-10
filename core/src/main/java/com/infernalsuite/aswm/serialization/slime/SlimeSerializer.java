package com.infernalsuite.aswm.serialization.slime;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.TagType;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import com.github.luben.zstd.Zstd;
import com.infernalsuite.aswm.api.utils.SlimeFormat;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.*;

public class SlimeSerializer {

    public static byte[] serialize(SlimeWorld world) {
        CompoundTag extraData = world.getExtraData();
        SlimePropertyMap propertyMap = world.getPropertyMap();

        // Store world properties
        if (!extraData.getValue().containsKey("properties")) {
            extraData.getValue().putIfAbsent("properties", propertyMap.toCompound());
        } else {
            extraData.getValue().replace("properties", propertyMap.toCompound());
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
            {
                byte[] extra = serializeCompoundTag(extraData);
                byte[] compressedExtra = Zstd.compress(extra);

                outStream.writeInt(compressedExtra.length);
                outStream.writeInt(extra.length);
                outStream.write(compressedExtra);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }


        return outByteStream.toByteArray();
    }

    static byte[] serializeChunks(SlimeWorld world, Collection<SlimeChunk> chunks) throws IOException {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        List<SlimeChunk> emptyChunks = new ArrayList<>(chunks);
        for (SlimeChunk chunk : chunks) {
            if (!ChunkPruner.canBePruned(world, chunk)) {
                emptyChunks.add(chunk);
            } else {
                System.out.println("PRUNED: " + chunk);
            }
        }

        outStream.writeInt(chunks.size());
        for (SlimeChunk chunk : emptyChunks) {
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
            ListTag<CompoundTag> tileEntitiesNbtList = new ListTag<>("tileEntities", TagType.TAG_COMPOUND, chunk.getTileEntities());
            CompoundTag tileEntitiesCompound = new CompoundTag("", new CompoundMap(Collections.singletonList(tileEntitiesNbtList)));
            byte[] tileEntitiesData = serializeCompoundTag(tileEntitiesCompound);
            byte[] compressedTileEntitiesData = Zstd.compress(tileEntitiesData);

            outStream.writeInt(compressedTileEntitiesData.length);
            outStream.writeInt(tileEntitiesData.length);
            outStream.write(compressedTileEntitiesData);

            // Entities
            ListTag<CompoundTag> entitiesNbtList = new ListTag<>("entities", TagType.TAG_COMPOUND, chunk.getEntities());
            CompoundTag entitiesCompound = new CompoundTag("", new CompoundMap(Collections.singletonList(entitiesNbtList)));
            byte[] entitiesData = serializeCompoundTag(entitiesCompound);
            byte[] compressedEntitiesData = Zstd.compress(entitiesData);

            outStream.writeInt(compressedEntitiesData.length);
            outStream.writeInt(entitiesData.length);
            outStream.write(compressedEntitiesData);
        }

        return outByteStream.toByteArray();
    }

    protected static byte[] serializeCompoundTag(CompoundTag tag) throws IOException {
        if (tag == null || tag.getValue().isEmpty()) {
            return new byte[0];
        }
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        NBTOutputStream outStream = new NBTOutputStream(outByteStream, NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
        outStream.writeTag(tag);

        return outByteStream.toByteArray();
    }

}
