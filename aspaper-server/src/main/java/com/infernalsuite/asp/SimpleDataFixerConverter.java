package com.infernalsuite.asp;

import ca.spottedleaf.dataconverter.converters.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCDataType;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.MapType;
import ca.spottedleaf.dataconverter.types.nbt.NBTListType;
import ca.spottedleaf.dataconverter.types.nbt.NBTMapType;
import com.infernalsuite.asp.api.SlimeDataConverter;
import com.infernalsuite.asp.level.SlimeChunkConverter;
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
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class SimpleDataFixerConverter implements SlimeWorldReader<SlimeWorld>, SlimeDataConverter {

    @Override
    public SlimeWorld readFromData(SlimeWorld data) {
        int newVersion = SharedConstants.getCurrentVersion().dataVersion().version();
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

            CompoundBinaryTag newPoi = chunk.getPoiChunkSections() != null ? convertPoiSections(chunk.getPoiChunkSections(), currentVersion, encodedCurrentVersion, encodedNewVersion) : null;

            chunks.put(chunkPos, new SlimeChunkSkeleton(
                    chunk.getX(),
                    chunk.getZ(),
                    sections,
                    chunk.getHeightMaps(),
                    blockEntities,
                    entities,
                    chunk.getExtraData(),
                    chunk.getUpgradeData(),
                    newPoi,
                    chunk.getBlockTicks(),
                    chunk.getFluidTicks()
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

    private CompoundBinaryTag convertPoiSections(CompoundBinaryTag poiChunkSections, int currentVersion, long encodedCurrentVersion, long encodedNewVersion) {
        CompoundTag poiChunk = SlimeChunkConverter.createPoiChunkFromSlimeSections(poiChunkSections, currentVersion);
        MCTypeRegistry.ENTITY.convert(new NBTMapType(poiChunk), encodedCurrentVersion, encodedNewVersion);
        return SlimeChunkConverter.getSlimeSectionsFromPoiCompound(poiChunk);
    }

    @Override
    public SlimeWorld applyDataFixers(SlimeWorld world) {
        return readFromData(world);
    }

    private static CompoundBinaryTag convertAndBack(CompoundBinaryTag value, Consumer<net.minecraft.nbt.CompoundTag> acceptor) {
        if (value == null) return null;

        net.minecraft.nbt.CompoundTag converted = (net.minecraft.nbt.CompoundTag) Converter.convertTag(value);
        acceptor.accept(converted);

        return Converter.convertTag(converted);
    }

    @Override
    public CompoundBinaryTag convertChunkTo1_13(CompoundBinaryTag tag) {
        CompoundTag nmsTag = (CompoundTag) Converter.convertTag(tag);

        int version = nmsTag.getInt("DataVersion").orElseThrow();

        long encodedNewVersion = DataConverter.encodeVersions(1631, Integer.MAX_VALUE);
        long encodedCurrentVersion = DataConverter.encodeVersions(version, Integer.MAX_VALUE);

        MCTypeRegistry.CHUNK.convert(new NBTMapType(nmsTag), encodedCurrentVersion, encodedNewVersion);

        return Converter.convertTag(nmsTag);
    }

    @Override
    public List<CompoundBinaryTag> convertEntities(List<CompoundBinaryTag> input, int from, int to) {
        List<CompoundBinaryTag> entities = new ArrayList<>(input.size());

        long encodedNewVersion = DataConverter.encodeVersions(to, Integer.MAX_VALUE);
        long encodedCurrentVersion = DataConverter.encodeVersions(from, Integer.MAX_VALUE);

        for (CompoundBinaryTag upgradeEntity : input) {
            entities.add(
                    convertAndBack(upgradeEntity, (tag) -> MCTypeRegistry.ENTITY.convert(new NBTMapType(tag), encodedCurrentVersion, encodedNewVersion))
            );
        }
        return entities;
    }

    @Override
    public List<CompoundBinaryTag> convertTileEntities(List<CompoundBinaryTag> input, int from, int to) {
        List<CompoundBinaryTag> blockEntities = new ArrayList<>(input.size());

        long encodedNewVersion = DataConverter.encodeVersions(to, Integer.MAX_VALUE);
        long encodedCurrentVersion = DataConverter.encodeVersions(from, Integer.MAX_VALUE);

        for (CompoundBinaryTag upgradeEntity : input) {
            blockEntities.add(
                    convertAndBack(upgradeEntity, (tag) -> MCTypeRegistry.TILE_ENTITY.convert(new NBTMapType(tag), encodedCurrentVersion, encodedNewVersion))
            );
        }
        return blockEntities;
    }

    @Override
    public ListBinaryTag convertBlockPalette(ListBinaryTag input, int from, int to) {
        long encodedNewVersion = DataConverter.encodeVersions(to, Integer.MAX_VALUE);
        long encodedCurrentVersion = DataConverter.encodeVersions(from, Integer.MAX_VALUE);

        ListTag nbtList = (ListTag) Converter.convertTag(input);
        NBTListType listType = new NBTListType(nbtList);

        for (int i = 0, len = listType.size(); i < len; ++i) {
            final MapType replace = MCTypeRegistry.BLOCK_STATE.convert(listType.getMap(i),
                    encodedCurrentVersion, encodedNewVersion);
            if (replace != null) {
                listType.setMap(i, replace);
            }
        }

        return Converter.convertTag(listType.getTag());
    }
}
