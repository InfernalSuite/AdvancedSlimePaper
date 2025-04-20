package com.infernalsuite.asp.serialization.slime.reader.impl.v1_9.upgrade;

import com.infernalsuite.asp.api.SlimeDataConverter;
import com.infernalsuite.asp.api.SlimeNMSBridge;
import com.infernalsuite.asp.serialization.slime.reader.impl.v1_9.v1_9SlimeChunkSection;
import com.infernalsuite.asp.serialization.slime.reader.impl.v1_9.v1_9SlimeWorld;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;

import java.util.Arrays;
import java.util.Map;

public class v1_16WorldUpgrade implements com.infernalsuite.asp.serialization.slime.reader.impl.v1_9.Upgrade {

    private static final int DATA_VERSION = 2586;

    private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[]{
            0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6, 11, 5, 10, 9
    };

    @Override
    public void upgrade(v1_9SlimeWorld world, SlimeDataConverter slimeDataConverter) {
        for (com.infernalsuite.asp.serialization.slime.reader.impl.v1_9.v1_9SlimeChunk chunk : world.chunks.values()) {
            chunk.tileEntities = slimeDataConverter.convertTileEntities(chunk.tileEntities, world.getDataVersion(), DATA_VERSION);
            chunk.entities = slimeDataConverter.convertEntities(chunk.entities, world.getDataVersion(), DATA_VERSION);


            // Add padding to height maps and block states
            CompoundBinaryTag heightMaps = CompoundBinaryTag.builder().build();

            for (Map.Entry<String, ? extends BinaryTag> heightMapEntry : chunk.heightMap) {
                if (heightMapEntry.getValue() instanceof LongArrayBinaryTag arrayTag) {
                    heightMaps.put(heightMapEntry.getKey(), LongArrayBinaryTag.longArrayBinaryTag(
                        addPadding(256, 9, arrayTag.value())
                    ));
                } else {
                    heightMaps.put(heightMapEntry.getKey(), heightMapEntry.getValue());
                }
            }

            for (int sectionIndex = 0; sectionIndex < chunk.sections.length; sectionIndex++) {
                v1_9SlimeChunkSection section = chunk.sections[sectionIndex];

                if (section != null) {
                    section.palette = slimeDataConverter.convertBlockPalette(section.palette, world.getDataVersion(), DATA_VERSION);

                    int bitsPerBlock = Math.max(4, ceillog2(section.palette.size()));

                    if (!isPowerOfTwo(bitsPerBlock)) {
                        section = new v1_9SlimeChunkSection(null, null, section.palette,
                                addPadding(4096, bitsPerBlock, section.blockStates), null, null,
                                section.blockLight, section.skyLight);
                        chunk.sections[sectionIndex] = section;
                    }
                }
            }

            // Update biome array size
            int[] newBiomes = new int[1024];
            Arrays.fill(newBiomes, -1);
            int[] biomes = chunk.biomes;
            System.arraycopy(biomes, 0, newBiomes, 0, biomes.length);

            chunk.biomes = newBiomes;
        }
    }

    private static int ceillog2(int input) {
        input = isPowerOfTwo(input) ? input : smallestEncompassingPowerOfTwo(input);
        return MULTIPLY_DE_BRUIJN_BIT_POSITION[(int) ((long) input * 125613361L >> 27) & 31];
    }

    private static int smallestEncompassingPowerOfTwo(int input) {
        int result = input - 1;
        result |= result >> 1;
        result |= result >> 2;
        result |= result >> 4;
        result |= result >> 8;
        result |= result >> 16;
        return result + 1;
    }

    private static boolean isPowerOfTwo(int input) {
        return input != 0 && (input & input - 1) == 0;
    }

    // Taken from DataConverterBitStorageAlign.java
    private static long[] addPadding(int indices, int bitsPerIndex, long[] originalArray) {
        int k = originalArray.length;

        if (k == 0) {
            return originalArray;
        }

        long l = (1L << bitsPerIndex) - 1L;
        int i1 = 64 / bitsPerIndex;
        int j1 = (indices + i1 - 1) / i1;
        long[] along1 = new long[j1];
        int k1 = 0;
        int l1 = 0;
        long i2 = 0L;
        int j2 = 0;
        long k2 = originalArray[0];
        long l2 = k > 1 ? originalArray[1] : 0L;

        for (int i3 = 0; i3 < indices; ++i3) {
            int j3 = i3 * bitsPerIndex;
            int k3 = j3 >> 6;
            int l3 = (i3 + 1) * bitsPerIndex - 1 >> 6;
            int i4 = j3 ^ k3 << 6;

            if (k3 != j2) {
                k2 = l2;
                l2 = k3 + 1 < k ? originalArray[k3 + 1] : 0L;
                j2 = k3;
            }

            long j4;
            int k4;

            if (k3 == l3) {
                j4 = k2 >>> i4 & l;
            } else {
                k4 = 64 - i4;
                j4 = (k2 >>> i4 | l2 << k4) & l;
            }

            k4 = l1 + bitsPerIndex;
            if (k4 >= 64) {
                along1[k1++] = i2;
                i2 = j4;
                l1 = bitsPerIndex;
            } else {
                i2 |= j4 << l1;
                l1 = k4;
            }
        }

        if (i2 != 0L) {
            along1[k1] = i2;
        }

        return along1;
    }
}