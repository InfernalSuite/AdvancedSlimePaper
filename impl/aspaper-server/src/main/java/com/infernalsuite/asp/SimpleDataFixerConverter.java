package com.infernalsuite.asp;

import ca.spottedleaf.dataconverter.converters.DataConverter;
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
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.SharedConstants;

import java.util.ArrayList;
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

        long encodedNewVersion = DataConverter.encodeVersions(newVersion, Integer.MAX_VALUE);
        long encodedCurrentVersion = DataConverter.encodeVersions(currentVersion, Integer.MAX_VALUE);

        Long2ObjectMap<SlimeChunk> chunks = new Long2ObjectOpenHashMap<>();
        for (SlimeChunk chunk : data.getChunkStorage()) {
            List<CompoundBinaryTag> entities = new ArrayList<>();
            List<CompoundBinaryTag> blockEntities = new ArrayList<>();
            for (CompoundBinaryTag upgradeEntity : chunk.getTileEntities()) {
                blockEntities.add(
                        convertAndBack(upgradeEntity, (tag) -> MCTypeRegistry.TILE_ENTITY.convert(new NBTMapType(tag), encodedCurrentVersion, encodedNewVersion))
                );
            }
            for (CompoundBinaryTag upgradeEntity : chunk.getEntities()) {
                entities.add(
                        convertAndBack(upgradeEntity, (tag) -> MCTypeRegistry.ENTITY.convert(new NBTMapType(tag), encodedCurrentVersion, encodedNewVersion))
                );
            }
            long chunkPos = Util.chunkPosition(chunk.getX(), chunk.getZ());

            SlimeChunkSection[] sections = new SlimeChunkSection[chunk.getSections().length];
            for (int i = 0; i < sections.length; i++) {
                SlimeChunkSection dataSection = chunk.getSections()[i];
                if (dataSection == null) continue;

                CompoundBinaryTag blockStateTag = convertAndBack(dataSection.getBlockStatesTag(), (tag) -> {
                    WalkerUtils.convertList(MCTypeRegistry.BLOCK_STATE, new NBTMapType(tag), "palette", encodedCurrentVersion, encodedNewVersion);
                });

                CompoundBinaryTag biomeTag = convertAndBack(dataSection.getBiomeTag(), (tag) -> {
                    WalkerUtils.convertList(MCTypeRegistry.BIOME, new NBTMapType(tag), "palette", encodedCurrentVersion, encodedNewVersion);
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
                    chunk.getExtraData(),
                    chunk.getUpgradeData()
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
