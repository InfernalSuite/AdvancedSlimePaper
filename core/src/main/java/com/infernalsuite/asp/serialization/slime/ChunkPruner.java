package com.infernalsuite.asp.serialization.slime;

import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.api.world.properties.SlimeProperties;
import com.infernalsuite.asp.api.world.properties.SlimePropertyMap;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;

import java.util.List;

public class ChunkPruner {

    public static boolean canBePruned(SlimeWorld world, SlimeChunk chunk) {
        SlimePropertyMap propertyMap = world.getPropertyMap();
        if (propertyMap.getValue(SlimeProperties.SHOULD_LIMIT_SAVE)) {
            int minX = propertyMap.getValue(SlimeProperties.SAVE_MIN_X);
            int maxX = propertyMap.getValue(SlimeProperties.SAVE_MAX_X);

            int minZ = propertyMap.getValue(SlimeProperties.SAVE_MIN_Z);
            int maxZ = propertyMap.getValue(SlimeProperties.SAVE_MAX_Z);

            int chunkX = chunk.getX();
            int chunkZ = chunk.getZ();

            if (chunkX < minX || chunkX > maxX) {
                return true;
            }

            if (chunkZ < minZ || chunkZ > maxZ) {
                return true;
            }
        }

        String pruningSetting = world.getPropertyMap().getValue(SlimeProperties.CHUNK_PRUNING);
        if (pruningSetting.equals("aggressive")) {
            return chunk.getTileEntities().isEmpty() && chunk.getEntities().isEmpty() && areSectionsEmpty(chunk.getSections());
        }

        return false;
    }

    //  TAG_List("palette"): 1 entries of type TAG_Compound
    //[13:15:06 INFO]:    {
    //[13:15:06 INFO]:       TAG_Compound: 1 entries
    //[13:15:06 INFO]:       {
    //[13:15:06 INFO]:          TAG_String("Name"): minecraft:air
    //[13:15:06 INFO]:       }
    private static boolean areSectionsEmpty(SlimeChunkSection[] sections) {
        for (SlimeChunkSection chunkSection : sections) {
            try {
                ListBinaryTag paletteTag = chunkSection.getBlockStatesTag().getList("palette");
                if (paletteTag.elementType() != BinaryTagTypes.COMPOUND) {
                    continue; // If the element type isn't a compound tag, consider the section empty
                }
                List<CompoundBinaryTag> palette = paletteTag.stream().map(tag -> (CompoundBinaryTag) tag).toList();
                if (palette.size() > 1) return false; // If there is more than one palette, the section is not empty
                if (palette.getFirst().getString("Name").equals("minecraft:air")) return false; // If the only palette entry is not air, the section is not empty
            } catch (final Exception e) {
                return false;
            }
            // The section is empty, continue to the next one
        }
        // All sections are empty, we can omit this chunk
        return true;
    }

}
