package com.infernalsuite.asp.serialization.anvil;

import com.github.luben.zstd.ZstdInputStream;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.util.Util;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.jpountz.lz4.LZ4BlockInputStream;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;

final class ExtraRegionReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtraRegionReader.class);

    private static final Pattern REGION_FILE = Pattern.compile("^r\\.(-?\\d+)\\.(-?\\d+)\\.mca$");

    private static final int SECTOR_SIZE = 4096;
    private static final int HEADER_SIZE = 2 * SECTOR_SIZE;
    private static final int CHUNK_HEADER_SIZE = 5;
    private static final int EXTERNAL_STREAM_FLAG = 0b1000_0000;

    private ExtraRegionReader() {
    }

    static void attachTo(Path directory, String extraDataKey, Long2ObjectMap<SlimeChunk> chunks) throws IOException {
        int attached = 0;
        int orphaned = 0;

        try (var stream = Files.newDirectoryStream(directory, path -> path.toString().endsWith(".mca"))) {
            for (final Path path : stream) {
                Matcher matcher = REGION_FILE.matcher(path.getFileName().toString());
                if (!matcher.matches()) continue;

                byte[] bytes = Files.readAllBytes(path);
                if (bytes.length < HEADER_SIZE) continue;

                LOGGER.info("Loading {} region file {}...", extraDataKey, path.getFileName());

                ByteBuffer region = ByteBuffer.wrap(bytes);
                int regionX = Integer.parseInt(matcher.group(1));
                int regionZ = Integer.parseInt(matcher.group(2));

                for (int index = 0; index < 1024; index++) {
                    int sectorInfo = region.getInt(index * 4);
                    if (sectorInfo == 0) continue;

                    int chunkX = (regionX << 5) + (index & 31);
                    int chunkZ = (regionZ << 5) + (index >> 5);

                    SlimeChunk chunk = chunks.get(Util.chunkPosition(chunkX, chunkZ));
                    if (chunk == null) {
                        orphaned++;
                        continue;
                    }

                    byte[] data = readChunk(directory, region, sectorInfo, chunkX, chunkZ);
                    if (data == null) continue;

                    chunk.getExtraData().put(extraDataKey, ByteArrayBinaryTag.byteArrayBinaryTag(data));
                    attached++;
                }
            }
        }

        LOGGER.info("Attached {} data to {} chunks", extraDataKey, attached);
        if (orphaned > 0) {
            LOGGER.warn("Dropped {} data of {} chunks that are not part of the imported world", extraDataKey, orphaned);
        }
    }

    private static byte[] readChunk(Path directory, ByteBuffer region, int sectorInfo, int chunkX, int chunkZ) throws IOException {
        int offset = ((sectorInfo >> 8) & 0xFFFFFF) * SECTOR_SIZE;
        if (offset < HEADER_SIZE || offset + CHUNK_HEADER_SIZE > region.capacity()) {
            LOGGER.warn("Chunk at {},{} points outside of the region file", chunkX, chunkZ);
            return null;
        }

        int length = region.getInt(offset) - 1;
        int flags = region.get(offset + 4) & 0xFF;

        InputStream source;
        if ((flags & EXTERNAL_STREAM_FLAG) != 0) {
            Path external = directory.resolve("c." + chunkX + "." + chunkZ + ".mcc");
            if (!Files.isRegularFile(external)) {
                LOGGER.warn("Chunk at {},{} refers to a missing external file {}", chunkX, chunkZ, external.getFileName());
                return null;
            }
            source = Files.newInputStream(external);
        } else if (length < 0 || offset + CHUNK_HEADER_SIZE + length > region.capacity()) {
            LOGGER.warn("Chunk at {},{} has a truncated stream", chunkX, chunkZ);
            return null;
        } else {
            source = new ByteArrayInputStream(region.array(), offset + CHUNK_HEADER_SIZE, length);
        }

        InputStream decompressor = decompress(flags & 0b111, source);
        if (decompressor == null) {
            source.close();
            LOGGER.warn("Chunk at {},{} uses an unsupported compression method {}", chunkX, chunkZ, flags & 0b111);
            return null;
        }

        byte[] data;
        try (decompressor) {
            data = decompressor.readAllBytes();
        }

        return switch ((flags >> 3) & 0b1111) {
            case 0 -> stripRootName(data, chunkX, chunkZ);
            case 1 -> data;
            default -> {
                LOGGER.warn("Chunk at {},{} uses an unsupported format version {}", chunkX, chunkZ, (flags >> 3) & 0b1111);
                yield null;
            }
        };
    }

    private static InputStream decompress(int compression, InputStream source) throws IOException {
        return switch (compression) {
            case 1 -> source;
            case 2 -> new InflaterInputStream(source);
            case 3 -> new GZIPInputStream(source);
            case 4 -> LZ4BlockInputStream.newBuilder().build(source);
            case 5 -> new ZstdInputStream(source);
            default -> null;
        };
    }

    private static byte[] stripRootName(byte[] data, int chunkX, int chunkZ) {
        if (data.length < 3 || data[0] != BinaryTagTypes.COMPOUND.id()) {
            LOGGER.warn("Chunk at {},{} does not hold a compound tag", chunkX, chunkZ);
            return null;
        }

        int payload = 3 + (((data[1] & 0xFF) << 8) | (data[2] & 0xFF));
        byte[] nameless = new byte[data.length - payload + 1];
        nameless[0] = data[0];
        System.arraycopy(data, payload, nameless, 1, nameless.length - 1);
        return nameless;
    }
}
