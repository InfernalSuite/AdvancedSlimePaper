package com.infernalsuite.aswm.serialization.slime;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.TagType;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import com.github.luben.zstd.Zstd;
import com.infernalsuite.aswm.utils.SlimeFormat;
import com.infernalsuite.aswm.world.SlimeChunk;
import com.infernalsuite.aswm.world.SlimeChunkSection;
import com.infernalsuite.aswm.world.SlimeWorld;
import com.infernalsuite.aswm.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.SlimeLogger;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
            outStream.write(SlimeFormat.SLIME_VERSION);

            // World version
            outStream.writeInt(world.getDataVersion());

            // Chunks
            byte[] chunkData = serializeChunks(world.getChunkStorage());
            byte[] compressedChunkData = Zstd.compress(chunkData);

            outStream.writeInt(compressedChunkData.length);
            outStream.writeInt(chunkData.length);
            outStream.write(compressedChunkData);

            // Tile entities

            List<CompoundTag> tileEntitiesList = new ArrayList<>();
            for (SlimeChunk chunk : world.getChunkStorage()) {
                tileEntitiesList.addAll(chunk.getTileEntities());
            }
            ListTag<CompoundTag> tileEntitiesNbtList = new ListTag<>("tiles", TagType.TAG_COMPOUND, tileEntitiesList);
            CompoundTag tileEntitiesCompound = new CompoundTag("", new CompoundMap(Collections.singletonList(tileEntitiesNbtList)));
            byte[] tileEntitiesData = serializeCompoundTag(tileEntitiesCompound);
            byte[] compressedTileEntitiesData = Zstd.compress(tileEntitiesData);

            outStream.writeInt(compressedTileEntitiesData.length);
            outStream.writeInt(tileEntitiesData.length);
            outStream.write(compressedTileEntitiesData);


            // Entities
            List<CompoundTag> entitiesList = world.getEntities();
            SlimeLogger.debug("entitiesList being serialized: " + entitiesList.size());
            outStream.writeBoolean(!entitiesList.isEmpty());

            if (!entitiesList.isEmpty()) {
                ListTag<CompoundTag> entitiesNbtList = new ListTag<>("entities", TagType.TAG_COMPOUND, entitiesList);
                CompoundTag entitiesCompound = new CompoundTag("", new CompoundMap(Collections.singletonList(entitiesNbtList)));
                byte[] entitiesData = serializeCompoundTag(entitiesCompound);
                byte[] compressedEntitiesData = Zstd.compress(entitiesData);

                outStream.writeInt(compressedEntitiesData.length);
                outStream.writeInt(entitiesData.length);
                outStream.write(compressedEntitiesData);
            }


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

    static byte[] serializeChunks(Collection<SlimeChunk> chunks) {
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream(16384);
        DataOutputStream outStream = new DataOutputStream(outByteStream);

        for (SlimeChunk chunk : chunks) {
            try {
                // Height Maps
                byte[] heightMaps = serializeCompoundTag(chunk.getHeightMaps());
                outStream.writeInt(heightMaps.length);
                outStream.write(heightMaps);

                // Chunk sections
                SlimeChunkSection[] sections = chunk.getSections();

                outStream.writeInt(sections.length);
                for (int i = 0; i < sections.length; i++) {
                    outStream.writeInt(chunk.getX());
                    outStream.writeInt(chunk.getZ());

                    SlimeChunkSection section = sections[i];

                    // Block Light
                    boolean hasBlockLight = section.getBlockLight() != null;
                    outStream.writeBoolean(hasBlockLight);

                    if (hasBlockLight) {
                        outStream.write(section.getBlockLight().getBacking());
                    }

                    // Sky Light
                    boolean hasSkyLight = section.getSkyLight() != null;
                    outStream.writeBoolean(hasSkyLight);

                    if (hasSkyLight) {
                        outStream.write(section.getSkyLight().getBacking());
                    }

                    // Block Data
                    byte[] serializedBlockStates = serializeCompoundTag(section.getBlockStatesTag());
                    outStream.writeInt(serializedBlockStates.length);
                    outStream.write(serializedBlockStates);

                    byte[] serializedBiomes = serializeCompoundTag(section.getBiomeTag());
                    outStream.writeInt(serializedBiomes.length);
                    outStream.write(serializedBiomes);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
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
