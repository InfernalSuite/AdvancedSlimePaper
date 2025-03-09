package com.infernalsuite.asp;

import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.nbt.NBTMapType;
import com.infernalsuite.asp.serialization.SlimeWorldReader;
import com.infernalsuite.asp.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.asp.skeleton.SlimeChunkSkeleton;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.api.world.SlimeWorld;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minecraft.SharedConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

class SimpleDataFixerConverter implements SlimeWorldReader<SlimeWorld> {

    @Override
    public SlimeWorld readFromData(SlimeWorld data) {
        int newVersion = SharedConstants.getCurrentVersion().getDataVersion().getVersion();
        int currentVersion = data.getDataVersion();
        // Already fixed
        if (currentVersion == newVersion) {
            return data;
        }

        Map<com.infernalsuite.asp.ChunkPos, SlimeChunk> chunks = new HashMap<>();
        for (SlimeChunk chunk : data.getChunkStorage()) {
            List<CompoundBinaryTag> entities = new ArrayList<>();
            List<CompoundBinaryTag> blockEntities = new ArrayList<>();
            for (CompoundBinaryTag upgradeEntity : chunk.getTileEntities()) {
                blockEntities.add(
                        convertAndBack(upgradeEntity, (tag) -> MCTypeRegistry.TILE_ENTITY.convert(new NBTMapType(tag), currentVersion, newVersion))
                );
            }
            for (CompoundBinaryTag upgradeEntity : chunk.getEntities()) {
                entities.add(
                        convertAndBack(upgradeEntity, (tag) -> MCTypeRegistry.ENTITY.convert(new NBTMapType(tag), currentVersion, newVersion))
                );
            }
            ChunkPos chunkPos = new ChunkPos(chunk.getX(), chunk.getZ());

            SlimeChunkSection[] sections = new SlimeChunkSection[chunk.getSections().length];
            for (int i = 0; i < sections.length; i++) {
                SlimeChunkSection dataSection = chunk.getSections()[i];
                if (dataSection == null) continue;

                CompoundBinaryTag blockStateTag = blockStateTag = convertAndBack(dataSection.getBlockStatesTag(), (tag) -> {
                    WalkerUtils.convertList(MCTypeRegistry.BLOCK_STATE, new NBTMapType(tag), "palette", currentVersion, newVersion);
                });

                CompoundBinaryTag biomeTag = convertAndBack(dataSection.getBiomeTag(), (tag) -> {
                    WalkerUtils.convertList(MCTypeRegistry.BIOME, new NBTMapType(tag), "palette", currentVersion, newVersion);
                });

                sections[i] = new SlimeChunkSectionSkeleton(
                        blockStateTag,
                        biomeTag,
                        dataSection.getBlockLight(),
                        dataSection.getSkyLight()
                );

            }
            chunks.put(chunkPos, new SlimeChunkSkeleton(
                    chunk.getX(),
                    chunk.getZ(),
                    sections,
                    chunk.getHeightMaps(),
                    blockEntities,
                    entities,
                    chunk.getExtraData()
            ));

        }

        return new SkeletonSlimeWorld(
                data.getName(),
                data.getLoader(),
                data.isReadOnly(),
                chunks,
                data.getExtraData(),
                data.getPropertyMap(),
                newVersion
        );
    }


    private static CompoundBinaryTag convertAndBack(CompoundBinaryTag value, Consumer<net.minecraft.nbt.CompoundTag> acceptor) {
        if (value == null) return null;

        net.minecraft.nbt.CompoundTag converted = (net.minecraft.nbt.CompoundTag) Converter.convertTag(value);
        acceptor.accept(converted);

        return Converter.convertTag(converted);
    }
}
