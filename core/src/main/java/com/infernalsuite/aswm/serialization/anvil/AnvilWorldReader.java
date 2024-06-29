package com.infernalsuite.aswm.serialization.anvil;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.ListTag;
import com.flowpowered.nbt.TagType;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.infernalsuite.aswm.Util;
import com.infernalsuite.aswm.api.exceptions.InvalidWorldException;
import com.infernalsuite.aswm.api.utils.NibbleArray;
import com.infernalsuite.aswm.api.world.SlimeChunk;
import com.infernalsuite.aswm.api.world.SlimeChunkSection;
import com.infernalsuite.aswm.api.world.SlimeWorld;
import com.infernalsuite.aswm.api.world.properties.SlimeProperties;
import com.infernalsuite.aswm.api.world.properties.SlimePropertyMap;
import com.infernalsuite.aswm.serialization.SlimeWorldReader;
import com.infernalsuite.aswm.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.aswm.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.aswm.skeleton.SlimeChunkSkeleton;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class AnvilWorldReader implements SlimeWorldReader<AnvilImportData> {

    public static final int V1_16 = 2566;
    public static final int V1_16_5 = 2586;
    public static final int V1_17_1 = 2730;
    public static final int V1_19_2 = 3120;

    private static final Pattern MAP_FILE_PATTERN = Pattern.compile("^(?:map_([0-9]*).dat)$");
    private static final int SECTOR_SIZE = 4096;

    public static final AnvilWorldReader INSTANCE = new AnvilWorldReader();

    @Override
    public SlimeWorld readFromData(AnvilImportData importData) {
        File worldDir = importData.worldDir();
        try {
            File levelFile = new File(worldDir, "level.dat");

            if (!levelFile.exists() || !levelFile.isFile()) {
                throw new RuntimeException(new InvalidWorldException(worldDir));
            }

            LevelData data = readLevelData(levelFile);

            // World version
            int worldVersion = data.version;

            SlimePropertyMap propertyMap = new SlimePropertyMap();

            File environmentDir = new File(worldDir, "DIM-1");
            propertyMap.setValue(SlimeProperties.ENVIRONMENT, "nether");
            if (!environmentDir.isDirectory()) {
                environmentDir = new File(worldDir, "DIM1");
                propertyMap.setValue(SlimeProperties.ENVIRONMENT, "the_end");
                if (!environmentDir.isDirectory()) {
                    environmentDir = worldDir;
                    propertyMap.setValue(SlimeProperties.ENVIRONMENT, "normal");
                }
            }

            // Chunks
            File regionDir = new File(environmentDir, "region");

            if (!regionDir.exists() || !regionDir.isDirectory()) {
                throw new InvalidWorldException(environmentDir);
            }

            Long2ObjectMap<SlimeChunk> chunks = new Long2ObjectOpenHashMap<>();

            for (File file : Objects.requireNonNull(regionDir.listFiles((dir, name) -> name.endsWith(".mca")))) {
                System.out.println("Loading region file: " + file.getName() + "...");
                if (file.exists()) {
                    chunks.putAll(
                            loadChunks(file, worldVersion).stream().collect(Collectors.toMap((chunk) -> Util.chunkPosition(chunk.getX(), chunk.getZ()), (chunk) -> chunk))
                    );
                }
            }

            // Entity serialization
            {
                File entityRegion = new File(environmentDir, "entities");
                if (entityRegion.exists()) {
                    for (File file : entityRegion.listFiles((dir, name) -> name.endsWith(".mca"))) {
                        if (file != null && file.exists()) {
                            loadEntities(file, worldVersion, chunks);
                        }
                    }
                }
            }

            if (chunks.isEmpty()) {
                throw new InvalidWorldException(environmentDir);
            }

            // World maps
//        File dataDir = new File(worldDir, "data");
//        List<CompoundTag> maps = new ArrayList<>();
//
//        if (dataDir.exists()) {
//            if (!dataDir.isDirectory()) {
//                throw new InvalidWorldException(worldDir);
//            }
//
//            for (File mapFile : dataDir.listFiles((dir, name) -> MAP_FILE_PATTERN.matcher(name).matches())) {
//                maps.add(loadMap(mapFile));
//            }
//        }

            // Extra Data
            CompoundMap extraData = new CompoundMap();

            propertyMap.setValue(SlimeProperties.SPAWN_X, data.x);
            propertyMap.setValue(SlimeProperties.SPAWN_Y, data.y);
            propertyMap.setValue(SlimeProperties.SPAWN_Z, data.z);

            return new SkeletonSlimeWorld(importData.newName(), importData.loader(), true, chunks, new CompoundTag("", extraData), propertyMap, worldVersion);
        } catch (IOException | InvalidWorldException e) {

            throw new RuntimeException(e);
        }
    }

    private static CompoundTag loadMap(File mapFile) throws IOException {
        String fileName = mapFile.getName();
        int mapId = Integer.parseInt(fileName.substring(4, fileName.length() - 4));
        CompoundTag tag;

        try (NBTInputStream nbtStream = new NBTInputStream(new FileInputStream(mapFile),
                NBTInputStream.GZIP_COMPRESSION, ByteOrder.BIG_ENDIAN)) {
            tag = nbtStream.readTag().getAsCompoundTag().get().getAsCompoundTag("data").get();
        }

        tag.getValue().put("id", new IntTag("id", mapId));

        return tag;
    }

    private static LevelData readLevelData(File file) throws IOException, InvalidWorldException {
        Optional<CompoundTag> tag;

        try (NBTInputStream nbtStream = new NBTInputStream(new FileInputStream(file))) {
            tag = nbtStream.readTag().getAsCompoundTag();
        }

        if (tag.isPresent()) {
            Optional<CompoundTag> dataTag = tag.get().getAsCompoundTag("Data");

            if (dataTag.isPresent()) {
                // Data version
                int dataVersion = dataTag.get().getIntValue("DataVersion").orElse(-1);

                int spawnX = dataTag.get().getIntValue("SpawnX").orElse(0);
                int spawnY = dataTag.get().getIntValue("SpawnY").orElse(255);
                int spawnZ = dataTag.get().getIntValue("SpawnZ").orElse(0);

                return new LevelData(dataVersion, spawnX, spawnY, spawnZ);
            }
        }

        throw new InvalidWorldException(file.getParentFile());
    }

    private static void loadEntities(File file, int version, Long2ObjectMap<SlimeChunk> chunkMap) throws IOException {
        byte[] regionByteArray = Files.readAllBytes(file.toPath());
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(regionByteArray));

        List<ChunkEntry> chunks = new ArrayList<>(1024);

        for (int i = 0; i < 1024; i++) {
            int entry = inputStream.readInt();
            int chunkOffset = entry >>> 8;
            int chunkSize = entry & 15;

            if (entry != 0) {
                ChunkEntry chunkEntry = new ChunkEntry(chunkOffset * SECTOR_SIZE, chunkSize * SECTOR_SIZE);
                chunks.add(chunkEntry);
            }
        }

        for (ChunkEntry entry : chunks) {
            try {
                DataInputStream headerStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.offset(), entry.paddedSize()));

                int chunkSize = headerStream.readInt() - 1;
                int compressionScheme = headerStream.readByte();

                DataInputStream chunkStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.offset() + 5, chunkSize));
                InputStream decompressorStream = compressionScheme == 1 ? new GZIPInputStream(chunkStream) : new InflaterInputStream(chunkStream);
                NBTInputStream nbtStream = new NBTInputStream(decompressorStream, NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
                CompoundTag globalCompound = (CompoundTag) nbtStream.readTag();
                CompoundMap globalMap = globalCompound.getValue();


                readEntityChunk(new CompoundTag("entityChunk", globalMap), version, chunkMap);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

    }

    private static List<SlimeChunk> loadChunks(File file, int worldVersion) throws IOException {
        byte[] regionByteArray = Files.readAllBytes(file.toPath());
        DataInputStream inputStream = new DataInputStream(new ByteArrayInputStream(regionByteArray));

        List<ChunkEntry> chunks = new ArrayList<>(1024);

        for (int i = 0; i < 1024; i++) {
            int entry = inputStream.readInt();
            int chunkOffset = entry >>> 8;
            int chunkSize = entry & 15;

            if (entry != 0) {
                ChunkEntry chunkEntry = new ChunkEntry(chunkOffset * SECTOR_SIZE, chunkSize * SECTOR_SIZE);
                chunks.add(chunkEntry);
            }
        }

        return chunks.stream().map((entry) -> {

            try {
                DataInputStream headerStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.offset(), entry.paddedSize()));

                int chunkSize = headerStream.readInt() - 1;
                int compressionScheme = headerStream.readByte();

                DataInputStream chunkStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.offset() + 5, chunkSize));
                InputStream decompressorStream = compressionScheme == 1 ? new GZIPInputStream(chunkStream) : new InflaterInputStream(chunkStream);
                NBTInputStream nbtStream = new NBTInputStream(decompressorStream, NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
                CompoundTag globalCompound = (CompoundTag) nbtStream.readTag();
                CompoundMap globalMap = globalCompound.getValue();

                CompoundTag levelDataTag = new CompoundTag("Level", globalMap);
                if (globalMap.containsKey("Level")) {
                    levelDataTag = (CompoundTag) globalMap.get("Level");
                }

                return readChunk(levelDataTag, worldVersion);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private static void readEntityChunk(CompoundTag compound, int worldVersion, Long2ObjectMap<SlimeChunk> slimeChunkMap) {
        int[] position = compound.getAsIntArrayTag("Position").orElseThrow().getValue();
        int chunkX = position[0];
        int chunkZ = position[1];

        int dataVersion = compound.getAsIntTag("DataVersion").map(IntTag::getValue).orElse(-1);
        if (dataVersion != worldVersion) {
            System.err.println("Cannot load entity chunk at " + chunkX + "," + chunkZ + ": data version " + dataVersion + " does not match world version " + worldVersion);
            return;
        }

        SlimeChunk chunk = slimeChunkMap.get(Util.chunkPosition(chunkX, chunkZ));
        if (chunk == null) {
            System.out.println("Lost entity chunk data at: " + chunkX + " " + chunkZ);
        } else {
            chunk.getEntities().addAll((List<CompoundTag>) compound.getAsListTag("Entities").get().getValue());
        }
    }

    private static SlimeChunk readChunk(CompoundTag compound, int worldVersion) {
        int chunkX = compound.getAsIntTag("xPos").get().getValue();
        int chunkZ = compound.getAsIntTag("zPos").get().getValue();

        if (worldVersion >= V1_19_2) { // 1.18 chunks should have a DataVersion tag, we can check if the chunk has been converted to match the world
            int dataVersion = compound.getAsIntTag("DataVersion").map(IntTag::getValue).orElse(-1);
            if (dataVersion != worldVersion) {
                System.err.println("Cannot load chunk at " + chunkX + "," + chunkZ + ": data version " + dataVersion + " does not match world version " + worldVersion);
                return null;
            }
        }

        Optional<String> status = compound.getStringValue("Status");

        if (status.isPresent() && !status.get().equals("postprocessed") && !status.get().startsWith("full") && !status.get().startsWith("minecraft:full")) {
            // It's a protochunk
            return null;
        }

//        int[] biomes;
//        Tag biomesTag = compound.getValue().get("Biomes");
//
//        if (biomesTag instanceof IntArrayTag) {
//            biomes = ((IntArrayTag) biomesTag).getValue();
//        } else if (biomesTag instanceof ByteArrayTag) {
//            byte[] byteBiomes = ((ByteArrayTag) biomesTag).getValue();
//            biomes = toIntArray(byteBiomes);
//        } else {
//            biomes = null;
//        }

        Optional<CompoundTag> optionalHeightMaps = compound.getAsCompoundTag("Heightmaps");
        CompoundTag heightMapsCompound = optionalHeightMaps.orElse(new CompoundTag("", new CompoundMap()));

        List<CompoundTag> tileEntities;
        List<CompoundTag> entities;
        ListTag<CompoundTag> sectionsTag;

        int minSectionY = 0;
        int maxSectionY = 16;

        if (worldVersion < V1_19_2) {
            tileEntities = ((ListTag<CompoundTag>) compound.getAsListTag("TileEntities")
                    .orElse(new ListTag<>("TileEntities", TagType.TAG_COMPOUND, new ArrayList<>()))).getValue();
            entities = ((ListTag<CompoundTag>) compound.getAsListTag("Entities")
                    .orElse(new ListTag<>("Entities", TagType.TAG_COMPOUND, new ArrayList<>()))).getValue();
            sectionsTag = (ListTag<CompoundTag>) compound.getAsListTag("Sections").get();
        } else {
            tileEntities = ((ListTag<CompoundTag>) compound.getAsListTag("block_entities")
                    .orElse(new ListTag<>("block_entities", TagType.TAG_COMPOUND, new ArrayList<>()))).getValue();
            entities = ((ListTag<CompoundTag>) compound.getAsListTag("entities")
                    .orElse(new ListTag<>("entities", TagType.TAG_COMPOUND, new ArrayList<>()))).getValue();
            sectionsTag = (ListTag<CompoundTag>) compound.getAsListTag("sections").get();

            Class<?> type = compound.getValue().get("yPos").getValue().getClass();

            if (type == Byte.class) {
                minSectionY = compound.getByteValue("yPos").orElseThrow();
            } else {
                minSectionY = compound.getIntValue("yPos").orElseThrow();
            }

            maxSectionY = sectionsTag.getValue().stream().map(c -> c.getByteValue("Y").orElseThrow()).max(Byte::compareTo).orElse((byte) 0) + 1; // Add 1 to the section, as we serialize it with the 1 added.
        }

        SlimeChunkSection[] sectionArray = new SlimeChunkSection[maxSectionY - minSectionY];

        for (CompoundTag sectionTag : sectionsTag.getValue()) {
            int index = sectionTag.getByteValue("Y").get();

            if (worldVersion < V1_17_1 && index < 0) {
                // For some reason MC 1.14 worlds contain an empty section with Y = -1, however 1.17+ worlds can use these sections
                continue;
            }

            ListTag<CompoundTag> paletteTag = null;
            long[] blockStatesArray = null;

            CompoundTag blockStatesTag = null;
            CompoundTag biomeTag = null;
            if (worldVersion < V1_19_2) {
                paletteTag = (ListTag<CompoundTag>) sectionTag.getAsListTag("Palette").orElse(null);
                blockStatesArray = sectionTag.getLongArrayValue("BlockStates").orElse(null);

                if (paletteTag == null || blockStatesArray == null || isEmpty(blockStatesArray)) { // Skip it
                    continue;
                }
            } else {
                if (!sectionTag.getAsCompoundTag("block_states").isPresent() && !sectionTag.getAsCompoundTag("biomes").isPresent()) {
                    continue; // empty section
                }
                blockStatesTag = sectionTag.getAsCompoundTag("block_states").orElseThrow();
                biomeTag = sectionTag.getAsCompoundTag("biomes").orElseThrow();
            }

            NibbleArray blockLightArray = sectionTag.getValue().containsKey("BlockLight") ? new NibbleArray(sectionTag.getByteArrayValue("BlockLight").get()) : null;
            NibbleArray skyLightArray = sectionTag.getValue().containsKey("SkyLight") ? new NibbleArray(sectionTag.getByteArrayValue("SkyLight").get()) : null;

            // There is no need to do any custom processing here.
            sectionArray[index - minSectionY] = new SlimeChunkSectionSkeleton(/*paletteTag, blockStatesArray,*/ blockStatesTag, biomeTag, blockLightArray, skyLightArray);
        }

        CompoundTag extraTag = new CompoundTag("", new CompoundMap());
        compound.getAsCompoundTag("ChunkBukkitValues").ifPresent(chunkBukkitValues -> extraTag.getValue().put(chunkBukkitValues));

        for (SlimeChunkSection section : sectionArray) {
            if (section != null) { // Chunk isn't empty
                return new SlimeChunkSkeleton(chunkX, chunkZ, sectionArray, heightMapsCompound, tileEntities, entities, extraTag, null);
            }
        }

        // Chunk is empty
        return null;
    }

    private static int[] toIntArray(byte[] buf) {
        ByteBuffer buffer = ByteBuffer.wrap(buf).order(ByteOrder.BIG_ENDIAN);
        int[] ret = new int[buf.length / 4];

        buffer.asIntBuffer().get(ret);

        return ret;
    }

    private static boolean isEmpty(byte[] array) {
        for (byte b : array) {
            if (b != 0) {
                return false;
            }
        }

        return true;
    }

    private static boolean isEmpty(long[] array) {
        for (long b : array) {
            if (b != 0L) {
                return false;
            }
        }

        return true;
    }


    private record ChunkEntry(int offset, int paddedSize) {

    }

    private record LevelData(int version, int x, int y, int z) {
    }
}
