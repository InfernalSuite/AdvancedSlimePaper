package com.grinderwolf.swm.plugin.upgrade.v1_16;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.LongArrayTag;
import com.flowpowered.nbt.Tag;
import com.grinderwolf.swm.api.world.SlimeChunk;
import com.grinderwolf.swm.api.world.SlimeChunkSection;
import com.grinderwolf.swm.nms.CraftSlimeChunk;
import com.grinderwolf.swm.nms.CraftSlimeChunkSection;
import com.grinderwolf.swm.nms.world.SlimeLoadedWorld;
import com.grinderwolf.swm.plugin.upgrade.Upgrade;

import java.util.ArrayList;
import java.util.Arrays;

public class v1_16WorldUpgrade implements Upgrade {

    private static final int[] MULTIPLY_DE_BRUIJN_BIT_POSITION = new int[] {
            0, 1, 28, 2, 29, 14, 24, 3, 30, 22, 20, 15, 25, 17, 4, 8, 31, 27, 13, 23, 21, 19, 16, 7, 26, 12, 18, 6,11, 5, 10, 9
    };

    @Override
    public void upgrade(SlimeLoadedWorld world) {
        for (SlimeChunk chunk : new ArrayList<>(world.getChunks().values())) {
            // Add padding to height maps and block states
            CompoundTag heightMaps = chunk.getHeightMaps();

            for (Tag<?> map : heightMaps.getValue().values()) {
                if (map instanceof LongArrayTag) {
                    LongArrayTag arrayTag = (LongArrayTag) map;
                    arrayTag.setValue(addPadding(256, 9, arrayTag.getValue()));
                }
            }

            for (int sectionIndex = 0; sectionIndex < chunk.getSections().length; sectionIndex++) {
                SlimeChunkSection section = chunk.getSections()[sectionIndex];

                if (section != null) {
                    int bitsPerBlock = Math.max(4, ceillog2(section.getPalette().getValue().size()));

                    if (!isPowerOfTwo(bitsPerBlock)) {
                        section = new CraftSlimeChunkSection(section.getPalette(),
                                addPadding(4096, bitsPerBlock, section.getBlockStates()), null, null,
                                section.getBlockLight(), section.getSkyLight());
                        chunk.getSections()[sectionIndex] = section;
                    }
                }
            }

            // Update biome array size
            int[] newBiomes = new int[1024];
            Arrays.fill(newBiomes, -1);
            int[] biomes = chunk.getBiomes();
            System.arraycopy(biomes, 0, newBiomes, 0, biomes.length);

            world.updateChunk(new CraftSlimeChunk(chunk.getWorldName(), chunk.getX(), chunk.getZ(),
                    chunk.getSections(), chunk.getHeightMaps(), newBiomes,
                    chunk.getTileEntities(), chunk.getEntities(), 0, 16));
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
