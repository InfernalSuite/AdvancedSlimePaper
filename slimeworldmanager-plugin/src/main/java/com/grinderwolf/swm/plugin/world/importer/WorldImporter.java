package com.grinderwolf.swm.plugin.world.importer;

import com.flowpowered.nbt.*;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.grinderwolf.swm.api.exceptions.InvalidWorldException;
import com.grinderwolf.swm.api.utils.NibbleArray;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.api.world.properties.SlimeProperties;
import com.grinderwolf.swm.api.world.properties.SlimePropertyMap;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeChunkSection;
import com.grinderwolf.swm.nms.NmsUtil;
import com.grinderwolf.swm.nms.world.SlimeLoadedWorld;
import com.grinderwolf.swm.plugin.SWMPlugin;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

public class WorldImporter {

    private static final Pattern MAP_FILE_PATTERN = Pattern.compile("^(?:map_([0-9]*).dat)$");
    private static final int SECTOR_SIZE = 4096;

    private static byte convertDataVersionToWorldVersion(int dataVersion) {
        if (dataVersion == -1) { // DataVersion tag was added in 1.9
            return 0x01;
        } else if (dataVersion < 818) {
            return 0x02; // 1.9 world
        } else if (dataVersion < 1501) {
            return 0x03; // 1.11 world
        } else if (dataVersion < 1517) {
            return 0x04; // 1.13 world
        } else if (dataVersion < 2566) {
            return 0x05; // 1.14 world
        } else if (dataVersion <= 2586) {
            return 0x06; // 1.16 world
        } else if (dataVersion <= 2730) {
            return 0x07; // 1.17 world
        } else if (dataVersion <= 3105) {
            return 0x08; // 1.18 & 1.19 world (world version did not change)
        } else {
            throw new UnsupportedOperationException("Unsupported world version: " + dataVersion);
        }
    }

    public static SlimeLoadedWorld readFromDirectory(File worldDir) throws InvalidWorldException, IOException {
        File levelFile = new File(worldDir, "level.dat");

        if (!levelFile.exists() || !levelFile.isFile()) {
            throw new InvalidWorldException(worldDir);
        }

        LevelData data = readLevelData(levelFile);

        // World version
        byte worldVersion = convertDataVersionToWorldVersion(data.getVersion());

        // Chunks
        File regionDir = new File(worldDir, "region");

        if (!regionDir.exists() || !regionDir.isDirectory()) {
            throw new InvalidWorldException(worldDir);
        }

        Long2ObjectOpenHashMap<SlimeChunk> chunks = new Long2ObjectOpenHashMap<>();

        for (File file : regionDir.listFiles((dir, name) -> name.endsWith(".mca"))) {
            chunks.putAll(loadChunks(file, worldVersion).stream().collect(Collectors.toMap((chunk) -> NmsUtil.asLong(chunk.getX(), chunk.getZ()), (chunk) -> chunk)));
        }

        if (chunks.isEmpty()) {
            throw new InvalidWorldException(worldDir);
        }

        // World maps
        File dataDir = new File(worldDir, "data");
        List<CompoundTag> maps = new ArrayList<>();

        if (dataDir.exists()) {
            if (!dataDir.isDirectory()) {
                throw new InvalidWorldException(worldDir);
            }

            for (File mapFile : dataDir.listFiles((dir, name) -> MAP_FILE_PATTERN.matcher(name).matches())) {
                maps.add(loadMap(mapFile));
            }
        }

        // Extra Data
        CompoundMap extraData = new CompoundMap();

        if (!data.getGameRules().isEmpty()) {
            CompoundMap gamerules = new CompoundMap();
            data.getGameRules().forEach((rule, value) -> gamerules.put(rule, new StringTag(rule, value)));

            extraData.put("gamerules", new CompoundTag("gamerules", gamerules));
        }

        SlimePropertyMap propertyMap = new SlimePropertyMap();

        propertyMap.setValue(SlimeProperties.SPAWN_X, data.getSpawnX());
        propertyMap.setValue(SlimeProperties.SPAWN_Y, data.getSpawnY());
        propertyMap.setValue(SlimeProperties.SPAWN_Z, data.getSpawnZ());

        if (worldVersion != SWMPlugin.getInstance().getNms().getWorldVersion()) {
            throw new UnsupportedOperationException("Please ensure that this world is not outdated.");
        }

        return SWMPlugin.getInstance().getNms().createSlimeWorld(null, worldDir.getName(), chunks, new CompoundTag("", extraData), maps, worldVersion, propertyMap, false, true, null);
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

                // Game rules
                Map<String, String> gameRules = new HashMap<>();
                Optional<CompoundTag> rulesList = dataTag.get().getAsCompoundTag("GameRules");

                rulesList.ifPresent(compoundTag -> compoundTag.getValue().forEach((ruleName, ruleTag) ->
                        gameRules.put(ruleName, ruleTag.getAsStringTag().get().getValue())));

                int spawnX = dataTag.get().getIntValue("SpawnX").orElse(0);
                int spawnY = dataTag.get().getIntValue("SpawnY").orElse(255);
                int spawnZ = dataTag.get().getIntValue("SpawnZ").orElse(0);

                return new LevelData(dataVersion, gameRules, spawnX, spawnY, spawnZ);
            }
        }

        throw new InvalidWorldException(file.getParentFile());
    }

    private static List<SlimeChunk> loadChunks(File file, byte worldVersion) throws IOException {
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
                DataInputStream headerStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.getOffset(), entry.getPaddedSize()));

                int chunkSize = headerStream.readInt() - 1;
                int compressionScheme = headerStream.readByte();

                DataInputStream chunkStream = new DataInputStream(new ByteArrayInputStream(regionByteArray, entry.getOffset() + 5, chunkSize));
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

    private static SlimeChunk readChunk(CompoundTag compound, byte worldVersion) {
        int chunkX = compound.getAsIntTag("xPos").get().getValue();
        int chunkZ = compound.getAsIntTag("zPos").get().getValue();

        if (worldVersion >= 0x08) { // 1.18 chunks should have a DataVersion tag, we can check if the chunk has been converted to match the world
            int dataVersion = convertDataVersionToWorldVersion(compound.getAsIntTag("DataVersion").map(IntTag::getValue).orElse(-1));
            if (dataVersion != worldVersion) {
                System.err.println("Cannot load chunk at " + chunkX + "," + chunkZ + ": data version " + dataVersion + " does not match world version " + worldVersion);
                return null;
            }
        }

        Optional<String> status = compound.getStringValue("Status");

        if (status.isPresent() && !status.get().equals("postprocessed") && !status.get().startsWith("full")) {
            // It's a protochunk
            return null;
        }

        int[] biomes;
        Tag biomesTag = compound.getValue().get("Biomes");

        if (biomesTag instanceof IntArrayTag) {
            biomes = ((IntArrayTag) biomesTag).getValue();
        } else if (biomesTag instanceof ByteArrayTag) {
            byte[] byteBiomes = ((ByteArrayTag) biomesTag).getValue();
            biomes = toIntArray(byteBiomes);
        } else {
            biomes = null;
        }

        Optional<CompoundTag> optionalHeightMaps = compound.getAsCompoundTag("Heightmaps");
        CompoundTag heightMapsCompound;

        if (worldVersion >= 0x04) {
            heightMapsCompound = optionalHeightMaps.orElse(new CompoundTag("", new CompoundMap()));
        } else {
            // Pre 1.13 world

            int[] heightMap = compound.getIntArrayValue("HeightMap").orElse(new int[256]);
            heightMapsCompound = new CompoundTag("", new CompoundMap());
            heightMapsCompound.getValue().put("heightMap", new IntArrayTag("heightMap", heightMap));
        }

        List<CompoundTag> tileEntities;
        List<CompoundTag> entities;
        ListTag<CompoundTag> sectionsTag;

        int minSectionY = 0;
        int maxSectionY = 16;

        if (worldVersion < 0x08) {
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

            minSectionY = compound.getIntValue("yPos").orElseThrow();
            maxSectionY = sectionsTag.getValue().stream().map(c -> c.getByteValue("Y").orElseThrow()).max(Byte::compareTo).orElse((byte) 0) + 1; // Add 1 to the section, as we serialize it with the 1 added.
        }
        SlimeChunkSection[] sectionArray = new SlimeChunkSection[maxSectionY - minSectionY];

        for (CompoundTag sectionTag : sectionsTag.getValue()) {
            int index = sectionTag.getByteValue("Y").get();

            if (worldVersion < 0x07 && index < 0) {
                // For some reason MC 1.14 worlds contain an empty section with Y = -1, however 1.17+ worlds can use these sections
                continue;
            }

            ListTag<CompoundTag> paletteTag = null;
            long[] blockStatesArray = null;

            CompoundTag blockStatesTag = null;
            CompoundTag biomeTag = null;
            if (worldVersion < 0x08) {
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
            sectionArray[index - minSectionY] = new CraftSlimeChunkSection(paletteTag, blockStatesArray, blockStatesTag, biomeTag, blockLightArray, skyLightArray);
        }

        for (SlimeChunkSection section : sectionArray) {
            if (section != null) { // Chunk isn't empty
                return new CraftSlimeChunk(null, chunkX, chunkZ, sectionArray, heightMapsCompound, biomes, tileEntities, entities, minSectionY, maxSectionY);
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

    @Getter
    @RequiredArgsConstructor
    private static class ChunkEntry {

        private final int offset;
        private final int paddedSize;

    }
}
