package com.grinderwolf.swm.nms.world;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.TagType;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import com.github.luben.zstd.Zstd;
import com.grinderwolf.swm.api.loaders.SlimeLoader;
import com.grinderwolf.swm.api.utils.SlimeFormat;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.NmsUtil;
import com.grinderwolf.swm.nms.SlimeLogger;
import com.grinderwolf.swm.nms.SlimeNMS;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static com.grinderwolf.swm.api.world.properties.SlimeProperties.*;

public abstract class AbstractSlimeNMSWorld extends AbstractSlimeLoadedWorld {

    protected final SlimeNMS nms;

    private final Object chunkAccessLock = new Object();

    protected AbstractSlimeNMSWorld(byte version, SlimeLoader loader, String name, Long2ObjectOpenHashMap<SlimeChunk> chunks,
                                    CompoundTag extraData, SlimePropertyMap propertyMap, boolean readOnly, boolean lock,
                                    Long2ObjectOpenHashMap<List<CompoundTag>> entities,
                                    SlimeNMS nms) {
        super(version, loader, name, chunks, extraData, propertyMap, readOnly, lock, entities);
        this.nms = nms;
    }

    public CompletableFuture<byte[]> serialize() throws IOException {
        List<SlimeChunk> sortedChunks;

        synchronized (chunkAccessLock) {
            sortedChunks = new ArrayList<>(chunks.values());
        }

        sortedChunks.sort(Comparator.comparingLong(chunk -> NmsUtil.asLong(chunk.getX(), chunk.getZ())));

        sortedChunks.removeIf(Objects::isNull); // Remove empty chunks to save space
        if (propertyMap.getValue(SHOULD_LIMIT_SAVE)) {
            int minX = propertyMap.getValue(SAVE_MIN_X);
            int maxX = propertyMap.getValue(SAVE_MAX_X);

            int minZ = propertyMap.getValue(SAVE_MIN_Z);
            int maxZ = propertyMap.getValue(SAVE_MAX_Z);

            sortedChunks.removeIf((chunk) -> {
                int chunkX = chunk.getX();
                int chunkZ = chunk.getZ();

                if (chunkX < minX || chunkX > maxX) {
                    return true;
                }

                if (chunkZ < minZ || chunkZ > maxZ) {
                    return true;
                }

                return false;
            });
        }

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
            outStream.writeByte(version);

            // Lowest chunk coordinates
            int minX = sortedChunks.stream().mapToInt(SlimeChunk::getX).min().orElse(0);
            int minZ = sortedChunks.stream().mapToInt(SlimeChunk::getZ).min().orElse(0);
            int maxX = sortedChunks.stream().mapToInt(SlimeChunk::getX).max().orElse(0);
            int maxZ = sortedChunks.stream().mapToInt(SlimeChunk::getZ).max().orElse(0);

            outStream.writeShort(minX);
            outStream.writeShort(minZ);

            // Width and depth
            int width = maxX - minX + 1;
            int depth = maxZ - minZ + 1;

            outStream.writeShort(width);
            outStream.writeShort(depth);

            // Chunk Bitmask
            BitSet chunkBitset = new BitSet(width * depth);

            for (SlimeChunk chunk : sortedChunks) {
                int bitsetIndex = (chunk.getZ() - minZ) * width + (chunk.getX() - minX);

                chunkBitset.set(bitsetIndex, true);
            }

            int chunkMaskSize = (int) Math.ceil((width * depth) / 8.0D);
            writeBitSetAsBytes(outStream, chunkBitset, chunkMaskSize);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return serializeChunks(sortedChunks, version).thenAccept((chunkSerialization) -> {
                    try {

                        // Chunks
                        byte[] chunkData = chunkSerialization.chunks();
                        byte[] compressedChunkData = Zstd.compress(chunkData);

                        outStream.writeInt(compressedChunkData.length);
                        outStream.writeInt(chunkData.length);
                        outStream.write(compressedChunkData);

                        // Tile entities

                        List<CompoundTag> tileEntitiesList = chunkSerialization.tileEntities();
                        ListTag<CompoundTag> tileEntitiesNbtList = new ListTag<>("tiles", TagType.TAG_COMPOUND, tileEntitiesList);
                        CompoundTag tileEntitiesCompound = new CompoundTag("", new CompoundMap(Collections.singletonList(tileEntitiesNbtList)));
                        byte[] tileEntitiesData = serializeCompoundTag(tileEntitiesCompound);
                        byte[] compressedTileEntitiesData = Zstd.compress(tileEntitiesData);

                        outStream.writeInt(compressedTileEntitiesData.length);
                        outStream.writeInt(tileEntitiesData.length);
                        outStream.write(compressedTileEntitiesData);


                        // Entities

                        List<CompoundTag> entitiesList = chunkSerialization.entities();
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

                        // World Maps
                        {
                            CompoundMap map = new CompoundMap();
                            map.put("maps", new ListTag<>("maps", TagType.TAG_COMPOUND, Collections.emptyList()));

                            CompoundTag mapsCompound = new CompoundTag("", map);

                            byte[] mapArray = serializeCompoundTag(mapsCompound);
                            byte[] compressedMapArray = Zstd.compress(mapArray);

                            outStream.writeInt(compressedMapArray.length);
                            outStream.writeInt(mapArray.length);
                            outStream.write(compressedMapArray);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .thenApply((v) -> outByteStream.toByteArray());
    }

    public abstract CompletableFuture<ChunkSerialization> serializeChunks(List<SlimeChunk> chunks, byte worldVersion) throws IOException;

    protected static void writeBitSetAsBytes(DataOutputStream outStream, BitSet set, int fixedSize) throws IOException {
        byte[] array = set.toByteArray();
        outStream.write(array);

        int chunkMaskPadding = fixedSize - array.length;

        for (int i = 0; i < chunkMaskPadding; i++) {
            outStream.write(0);
        }
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
