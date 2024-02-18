package com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.upgrade;

import com.flowpowered.nbt.*;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.Upgrade;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.v1_9SlimeChunk;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.v1_9SlimeChunkSection;
import com.infernalsuite.aswm.serialization.slime.reader.impl.v1_9.v1_9SlimeWorld;

import java.util.*;

public class v1_18WorldUpgrade implements Upgrade {

    private static final String[] BIOMES_BY_ID = new String[256]; // rip datapacks

    static {
        BIOMES_BY_ID[0] = "minecraft:ocean";
        BIOMES_BY_ID[1] = "minecraft:plains";
        BIOMES_BY_ID[2] = "minecraft:desert";
        BIOMES_BY_ID[3] = "minecraft:mountains";
        BIOMES_BY_ID[4] = "minecraft:forest";
        BIOMES_BY_ID[5] = "minecraft:taiga";
        BIOMES_BY_ID[6] = "minecraft:swamp";
        BIOMES_BY_ID[7] = "minecraft:river";
        BIOMES_BY_ID[8] = "minecraft:nether_wastes";
        BIOMES_BY_ID[9] = "minecraft:the_end";
        BIOMES_BY_ID[10] = "minecraft:frozen_ocean";
        BIOMES_BY_ID[11] = "minecraft:frozen_river";
        BIOMES_BY_ID[12] = "minecraft:snowy_tundra";
        BIOMES_BY_ID[13] = "minecraft:snowy_mountains";
        BIOMES_BY_ID[14] = "minecraft:mushroom_fields";
        BIOMES_BY_ID[15] = "minecraft:mushroom_field_shore";
        BIOMES_BY_ID[16] = "minecraft:beach";
        BIOMES_BY_ID[17] = "minecraft:desert_hills";
        BIOMES_BY_ID[18] = "minecraft:wooded_hills";
        BIOMES_BY_ID[19] = "minecraft:taiga_hills";
        BIOMES_BY_ID[20] = "minecraft:mountain_edge";
        BIOMES_BY_ID[21] = "minecraft:jungle";
        BIOMES_BY_ID[22] = "minecraft:jungle_hills";
        BIOMES_BY_ID[23] = "minecraft:jungle_edge";
        BIOMES_BY_ID[24] = "minecraft:deep_ocean";
        BIOMES_BY_ID[25] = "minecraft:stone_shore";
        BIOMES_BY_ID[26] = "minecraft:snowy_beach";
        BIOMES_BY_ID[27] = "minecraft:birch_forest";
        BIOMES_BY_ID[28] = "minecraft:birch_forest_hills";
        BIOMES_BY_ID[29] = "minecraft:dark_forest";
        BIOMES_BY_ID[30] = "minecraft:snowy_taiga";
        BIOMES_BY_ID[31] = "minecraft:snowy_taiga_hills";
        BIOMES_BY_ID[32] = "minecraft:giant_tree_taiga";
        BIOMES_BY_ID[33] = "minecraft:giant_tree_taiga_hills";
        BIOMES_BY_ID[34] = "minecraft:wooded_mountains";
        BIOMES_BY_ID[35] = "minecraft:savanna";
        BIOMES_BY_ID[36] = "minecraft:savanna_plateau";
        BIOMES_BY_ID[37] = "minecraft:badlands";
        BIOMES_BY_ID[38] = "minecraft:wooded_badlands_plateau";
        BIOMES_BY_ID[39] = "minecraft:badlands_plateau";
        BIOMES_BY_ID[40] = "minecraft:small_end_islands";
        BIOMES_BY_ID[41] = "minecraft:end_midlands";
        BIOMES_BY_ID[42] = "minecraft:end_highlands";
        BIOMES_BY_ID[43] = "minecraft:end_barrens";
        BIOMES_BY_ID[44] = "minecraft:warm_ocean";
        BIOMES_BY_ID[45] = "minecraft:lukewarm_ocean";
        BIOMES_BY_ID[46] = "minecraft:cold_ocean";
        BIOMES_BY_ID[47] = "minecraft:deep_warm_ocean";
        BIOMES_BY_ID[48] = "minecraft:deep_lukewarm_ocean";
        BIOMES_BY_ID[49] = "minecraft:deep_cold_ocean";
        BIOMES_BY_ID[50] = "minecraft:deep_frozen_ocean";
        BIOMES_BY_ID[127] = "minecraft:the_void";
        BIOMES_BY_ID[129] = "minecraft:sunflower_plains";
        BIOMES_BY_ID[130] = "minecraft:desert_lakes";
        BIOMES_BY_ID[131] = "minecraft:gravelly_mountains";
        BIOMES_BY_ID[132] = "minecraft:flower_forest";
        BIOMES_BY_ID[133] = "minecraft:taiga_mountains";
        BIOMES_BY_ID[134] = "minecraft:swamp_hills";
        BIOMES_BY_ID[140] = "minecraft:ice_spikes";
        BIOMES_BY_ID[149] = "minecraft:modified_jungle";
        BIOMES_BY_ID[151] = "minecraft:modified_jungle_edge";
        BIOMES_BY_ID[155] = "minecraft:tall_birch_forest";
        BIOMES_BY_ID[156] = "minecraft:tall_birch_hills";
        BIOMES_BY_ID[157] = "minecraft:dark_forest_hills";
        BIOMES_BY_ID[158] = "minecraft:snowy_taiga_mountains";
        BIOMES_BY_ID[160] = "minecraft:giant_spruce_taiga";
        BIOMES_BY_ID[161] = "minecraft:giant_spruce_taiga_hills";
        BIOMES_BY_ID[162] = "minecraft:modified_gravelly_mountains";
        BIOMES_BY_ID[163] = "minecraft:shattered_savanna";
        BIOMES_BY_ID[164] = "minecraft:shattered_savanna_plateau";
        BIOMES_BY_ID[165] = "minecraft:eroded_badlands";
        BIOMES_BY_ID[166] = "minecraft:modified_wooded_badlands_plateau";
        BIOMES_BY_ID[167] = "minecraft:modified_badlands_plateau";
        BIOMES_BY_ID[168] = "minecraft:bamboo_jungle";
        BIOMES_BY_ID[169] = "minecraft:bamboo_jungle_hills";
        BIOMES_BY_ID[170] = "minecraft:soul_sand_valley";
        BIOMES_BY_ID[171] = "minecraft:crimson_forest";
        BIOMES_BY_ID[172] = "minecraft:warped_forest";
        BIOMES_BY_ID[173] = "minecraft:basalt_deltas";
        BIOMES_BY_ID[174] = "minecraft:dripstone_caves";
        BIOMES_BY_ID[175] = "minecraft:lush_caves";
        BIOMES_BY_ID[177] = "minecraft:meadow";
        BIOMES_BY_ID[178] = "minecraft:grove";
        BIOMES_BY_ID[179] = "minecraft:snowy_slopes";
        BIOMES_BY_ID[180] = "minecraft:snowcapped_peaks";
        BIOMES_BY_ID[181] = "minecraft:lofty_peaks";
        BIOMES_BY_ID[182] = "minecraft:stony_peaks";
    }

    public static final Map<String, String> BIOME_UPDATE = new HashMap<>();

    static {
        BIOME_UPDATE.put("minecraft:badlands_plateau", "minecraft:badlands");
        BIOME_UPDATE.put("minecraft:bamboo_jungle_hills", "minecraft:bamboo_jungle");
        BIOME_UPDATE.put("minecraft:birch_forest_hills", "minecraft:birch_forest");
        BIOME_UPDATE.put("minecraft:dark_forest_hills", "minecraft:dark_forest");
        BIOME_UPDATE.put("minecraft:desert_hills", "minecraft:desert");
        BIOME_UPDATE.put("minecraft:desert_lakes", "minecraft:desert");
        BIOME_UPDATE.put("minecraft:giant_spruce_taiga_hills", "minecraft:old_growth_spruce_taiga");
        BIOME_UPDATE.put("minecraft:giant_spruce_taiga", "minecraft:old_growth_spruce_taiga");
        BIOME_UPDATE.put("minecraft:giant_tree_taiga_hills", "minecraft:old_growth_pine_taiga");
        BIOME_UPDATE.put("minecraft:giant_tree_taiga", "minecraft:old_growth_pine_taiga");
        BIOME_UPDATE.put("minecraft:gravelly_mountains", "minecraft:windswept_gravelly_hills");
        BIOME_UPDATE.put("minecraft:jungle_edge", "minecraft:sparse_jungle");
        BIOME_UPDATE.put("minecraft:jungle_hills", "minecraft:jungle");
        BIOME_UPDATE.put("minecraft:modified_badlands_plateau", "minecraft:badlands");
        BIOME_UPDATE.put("minecraft:modified_gravelly_mountains", "minecraft:windswept_gravelly_hills");
        BIOME_UPDATE.put("minecraft:modified_jungle_edge", "minecraft:sparse_jungle");
        BIOME_UPDATE.put("minecraft:modified_jungle", "minecraft:jungle");
        BIOME_UPDATE.put("minecraft:modified_wooded_badlands_plateau", "minecraft:wooded_badlands");
        BIOME_UPDATE.put("minecraft:mountain_edge", "minecraft:windswept_hills");
        BIOME_UPDATE.put("minecraft:mountains", "minecraft:windswept_hills");
        BIOME_UPDATE.put("minecraft:mushroom_field_shore", "minecraft:mushroom_fields");
        BIOME_UPDATE.put("minecraft:shattered_savanna", "minecraft:windswept_savanna");
        BIOME_UPDATE.put("minecraft:shattered_savanna_plateau", "minecraft:windswept_savanna");
        BIOME_UPDATE.put("minecraft:snowy_mountains", "minecraft:snowy_plains");
        BIOME_UPDATE.put("minecraft:snowy_taiga_hills", "minecraft:snowy_taiga");
        BIOME_UPDATE.put("minecraft:snowy_taiga_mountains", "minecraft:snowy_taiga");
        BIOME_UPDATE.put("minecraft:snowy_tundra", "minecraft:snowy_plains");
        BIOME_UPDATE.put("minecraft:stone_shore", "minecraft:stony_shore");
        BIOME_UPDATE.put("minecraft:swamp_hills", "minecraft:swamp");
        BIOME_UPDATE.put("minecraft:taiga_hills", "minecraft:taiga");
        BIOME_UPDATE.put("minecraft:taiga_mountains", "minecraft:taiga");
        BIOME_UPDATE.put("minecraft:tall_birch_forest", "minecraft:old_growth_birch_forest");
        BIOME_UPDATE.put("minecraft:tall_birch_hills", "minecraft:old_growth_birch_forest");
        BIOME_UPDATE.put("minecraft:wooded_badlands_plateau", "minecraft:wooded_badlands");
        BIOME_UPDATE.put("minecraft:wooded_hills", "minecraft:forest");
        BIOME_UPDATE.put("minecraft:wooded_mountains", "minecraft:windswept_forest");
        BIOME_UPDATE.put("minecraft:lofty_peaks", "minecraft:jagged_peaks");
        BIOME_UPDATE.put("minecraft:snowcapped_peaks", "minecraft:frozen_peaks");
    }

    @Override
    public void upgrade(v1_9SlimeWorld world) {
        for (v1_9SlimeChunk chunk : world.chunks.values()) {

            // SpawnerSpawnDataFix
            for (CompoundTag tileEntity : chunk.tileEntities) {
                CompoundMap value = tileEntity.getValue();
                Optional<String> id = tileEntity.getStringValue("id");
                if (id.equals(Optional.of("minecraft:mob_spawner"))) {
                    Optional<ListTag<?>> spawnPotentials = tileEntity.getAsListTag("SpawnPotentials");
                    Optional<CompoundTag> spawnData = tileEntity.getAsCompoundTag("SpawnData");
                    if (spawnPotentials.isPresent()) {
                        ListTag<CompoundTag> spawnPotentialsList = (ListTag<CompoundTag>) spawnPotentials.get();
                        List<CompoundTag> spawnPotentialsListValue = spawnPotentialsList.getValue();
                        for (CompoundTag spawnPotentialsTag : spawnPotentialsListValue) {
                            CompoundMap spawnPotentialsValue = spawnPotentialsTag.getValue();
                            Optional<Integer> weight = spawnPotentialsTag.getIntValue("Weight");
                            if (weight.isPresent()) {
                                int weightVal = weight.get();
                                spawnPotentialsValue.remove("Weight");
                                spawnPotentialsValue.put("weight", new IntTag("weight", weightVal));
                            }
                            Optional<CompoundTag> entity = spawnPotentialsTag.getAsCompoundTag("Entity");
                            if (entity.isPresent()) {
                                CompoundTag entityTag = entity.get();
                                spawnPotentialsValue.remove("Entity");
                                entityTag.getValue();
                                CompoundMap dataMap = new CompoundMap();
                                dataMap.put(new CompoundTag("entity", entityTag.getValue()));
                                spawnPotentialsValue.put("data", new CompoundTag("data", dataMap));
                            }
                        }
                        value.put("SpawnPotentials", spawnPotentialsList);
                        if (!spawnPotentialsListValue.isEmpty()) {
                            CompoundTag compoundTag = spawnPotentialsListValue.get(0);
                            CompoundTag entityTag = compoundTag.getAsCompoundTag("data").
                                    get().getAsCompoundTag("entity").get();
                            CompoundMap spawnDataMap = new CompoundMap();
                            spawnDataMap.put(entityTag.clone());
                            value.put("SpawnData", new CompoundTag("SpawnData", spawnDataMap));
                        }
                    } else if (spawnData.isPresent()) {
                        CompoundTag spawnDataTag = spawnData.get();
                        CompoundMap spawnDataValue = spawnDataTag.getValue();
                        Optional<CompoundTag> entityTag = spawnDataTag.getAsCompoundTag("entity");
                        Optional<StringTag> idTag = spawnDataTag.getAsStringTag("id");
                        if (entityTag.isEmpty() && idTag.isPresent()) {
                            StringTag entityTypeTag = idTag.get();
                            spawnDataValue.remove("id");
                            CompoundMap entityMap = new CompoundMap();
                            entityMap.put(entityTypeTag);
                            spawnDataValue.put("entity", new CompoundTag("entity", entityMap));
                            value.put("SpawnData", spawnDataTag);
                        }
                    }
                }
            }

            CompoundTag[] tags = createBiomeSections(chunk.biomes, false, 0);

            v1_9SlimeChunkSection[] sections = chunk.sections;
            for (int i = 0; i < sections.length; i++) {
                v1_9SlimeChunkSection section = sections[i];
                if (section == null) {
                    continue;
                }


                section.blockStatesTag = wrapPalette(section.palette, section.blockStates);
                section.biomeTag = tags[i];
            }

            v1_9SlimeChunkSection[] shiftedSections = new v1_9SlimeChunkSection[sections.length + 4];
            System.arraycopy(sections, 0, shiftedSections, 4, sections.length);

            chunk.sections = shiftedSections; // Shift all sections up 4


            v1_9SlimeChunkSection[] sectionArray = chunk.sections;

            CompoundMap emptyBiomes = new CompoundMap();
            emptyBiomes.put("palette", new ListTag<>("palette", TagType.TAG_STRING, List.of(new StringTag("", "minecraft:plains"))));

            CompoundMap blocks = new CompoundMap();
            emptyBiomes.put("palette", new ListTag<>("palette", TagType.TAG_STRING, List.of(new StringTag("", "minecraft:air"))));

            CompoundTag blockTag = new CompoundTag("", blocks);
            CompoundTag emptyBiomesTag = new CompoundTag("", emptyBiomes);
            for (int i = 0; i < sectionArray.length; i++) {
                v1_9SlimeChunkSection section = sectionArray[i];
                if (section == null) {
                    sectionArray[i] = new v1_9SlimeChunkSection(
                            null,
                            null,
                            null,
                            null,
                            blockTag,
                            emptyBiomesTag,
                            null,
                            null
                    );
                }
            }
        }
    }

    private static CompoundTag[] createBiomeSections(int[] biomes, final boolean wantExtendedHeight, final int minSection) {
        final CompoundTag[] ret = new CompoundTag[wantExtendedHeight ? 24 : 16];

        if (biomes != null && biomes.length == 1536) { // magic value for 24 sections of biomes (24 * 4^3)
            //isAlreadyExtended.setValue(true);
            for (int sectionIndex = 0; sectionIndex < 24; ++sectionIndex) {
                ret[sectionIndex] = createBiomeSection(biomes, sectionIndex * 64, -1); // -1 is all 1s
            }
        } else if (biomes != null && biomes.length == 1024) { // magic value for 24 sections of biomes (16 * 4^3)
            for (int sectionY = 0; sectionY < 16; ++sectionY) {
                ret[sectionY - minSection] = createBiomeSection(biomes, sectionY * 64, -1); // -1 is all 1s
            }

//            if (wantExtendedHeight) {
//                // must set the new sections at top and bottom
//                final MapType<String> bottomCopy = createBiomeSection(biomes, 0, 15); // just want the biomes at y = 0
//                final MapType<String> topCopy = createBiomeSection(biomes, 1008, 15); // just want the biomes at y = 252
//
//                for (int sectionIndex = 0; sectionIndex < 4; ++sectionIndex) {
//                    ret[sectionIndex] = bottomCopy.copy(); // copy palette so that later possible modifications don't trash all sections
//                }
//
//                for (int sectionIndex = 20; sectionIndex < 24; ++sectionIndex) {
//                    ret[sectionIndex] = topCopy.copy(); // copy palette so that later possible modifications don't trash all sections
//                }
//            }
        } else {
            ArrayList<StringTag> palette = new ArrayList<>();
            palette.add(new StringTag("", "minecraft:plains"));

            for (int i = 0; i < ret.length; ++i) {
                ret[i] = wrapPalette(new ListTag<>("", TagType.TAG_STRING, palette).clone(), null); // copy palette so that later possible modifications don't trash all sections
            }
        }

        return ret;
    }

    public static int ceilLog2(final int value) {
        return value == 0 ? 0 : Integer.SIZE - Integer.numberOfLeadingZeros(value - 1); // see doc of numberOfLeadingZeros
    }

    private static CompoundTag createBiomeSection(final int[] biomes, final int offset, final int mask) {
        final Map<Integer, Integer> paletteId = new HashMap<>();

        for (int idx = 0; idx < 64; ++idx) {
            final int biome = biomes[offset + (idx & mask)];
            paletteId.putIfAbsent(biome, paletteId.size());
        }

        List<StringTag> paletteString = new ArrayList<>();
        for (final Iterator<Integer> iterator = paletteId.keySet().iterator(); iterator.hasNext(); ) {
            final int biomeId = iterator.next();
            String biome = biomeId >= 0 && biomeId < BIOMES_BY_ID.length ? BIOMES_BY_ID[biomeId] : null;
            String update = BIOME_UPDATE.get(biome);
            if (update != null) {
                biome = update;
            }

            paletteString.add(new StringTag("", biome == null ? "minecraft:plains" : biome));
        }

        final int bitsPerObject = ceilLog2(paletteString.size());
        if (bitsPerObject == 0) {
            return wrapPalette(new ListTag<>("", TagType.TAG_STRING, paletteString), null);
        }

        // manually create packed integer data
        final int objectsPerValue = 64 / bitsPerObject;
        final long[] packed = new long[(64 + objectsPerValue - 1) / objectsPerValue];

        int shift = 0;
        int idx = 0;
        long curr = 0;

        for (int biome_idx = 0; biome_idx < 64; ++biome_idx) {
            final int biome = biomes[offset + (biome_idx & mask)];

            curr |= ((long) paletteId.get(biome)) << shift;

            shift += bitsPerObject;

            if (shift + bitsPerObject > 64) { // will next write overflow?
                // must move to next idx
                packed[idx++] = curr;
                shift = 0;
                curr = 0L;
            }
        }

        // don't forget to write the last one
        if (shift != 0) {
            packed[idx] = curr;
        }

        return wrapPalette(new ListTag<>("", TagType.TAG_STRING, paletteString), packed);
    }

    private static CompoundTag wrapPalette(ListTag<?> palette, final long[] blockStates) {
        CompoundMap map = new CompoundMap();
        CompoundTag tag = new CompoundTag("", map);

        map.put(new ListTag<>("palette", palette.getElementType(), palette.getValue()));
        if (blockStates != null) {
            map.put(new LongArrayTag("data", blockStates));
        }

        return tag;
    }

}