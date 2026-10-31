package com.infernalsuite.asp.data;

import ca.spottedleaf.converter.DataConverter;
import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCDataType;
import ca.spottedleaf.dataconverter.minecraft.datatypes.MCTypeRegistry;
import ca.spottedleaf.dataconverter.minecraft.versions.V99;
import ca.spottedleaf.dataconverter.minecraft.walkers.generic.WalkerUtils;
import ca.spottedleaf.dataconverter.types.nbt.NBTListType;
import ca.spottedleaf.dataconverter.types.nbt.NBTMapType;
import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.api.SlimeDataConverter;
import com.infernalsuite.asp.level.chunk.SlimeChunkConverter;
import com.infernalsuite.asp.serialization.SlimeWorldReader;
import com.infernalsuite.asp.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.asp.skeleton.SlimeChunkSkeleton;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.util.Util;
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

public class SimpleDataFixerConverter implements SlimeWorldReader<SlimeWorld>, SlimeDataConverter {

    //Is DataConverter supported this version?
    public static final boolean SUPPORTED_RIGHT_NOW = true;

    @Override
    public SlimeWorld readFromData(SlimeWorld data) {
        int newVersion = SharedConstants.getCurrentVersion().dataVersion().version();
        int currentVersion = data.getDataVersion();
        // Already fixed
        if (currentVersion >= newVersion) {
            return data;
        }

        long encodedNewVersion = DataConverter.encodeVersions(newVersion, Integer.MAX_VALUE);
        long encodedCurrentVersion = DataConverter.encodeVersions(Math.max(currentVersion, V99.VERSION), Integer.MAX_VALUE);

        Long2ObjectMap<SlimeChunk> chunks = new Long2ObjectOpenHashMap<>();
        for (SlimeChunk chunk : data.getChunkStorage()) {
            List<CompoundBinaryTag> entities = new ArrayList<>();
            List<CompoundBinaryTag> blockEntities = new ArrayList<>();
            for (CompoundBinaryTag upgradeEntity : chunk.getTileEntities()) {
                blockEntities.add(
                        upgradeTagEncodedVersions(upgradeEntity, MCTypeRegistry.TILE_ENTITY, encodedCurrentVersion, encodedNewVersion)
                );
            }
            for (CompoundBinaryTag upgradeEntity : chunk.getEntities()) {
                entities.add(
                        upgradeTagEncodedVersions(upgradeEntity, MCTypeRegistry.ENTITY, encodedCurrentVersion, encodedNewVersion)
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

            CompoundBinaryTag newPoi = chunk.getPoiChunkSections() != null ? convertPoiSections(chunk.getPoiChunkSections(),
                    currentVersion, encodedCurrentVersion, encodedNewVersion) : null;

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
        poiChunk = upgradeTagEncodedVersionsNMS(poiChunk, MCTypeRegistry.POI_CHUNK, encodedCurrentVersion, encodedNewVersion);

        return SlimeChunkConverter.getSlimeSectionsFromPoiCompound(poiChunk);
    }

    @Override
    public SlimeWorld applyDataFixers(SlimeWorld world) {
        return readFromData(world);
    }

    private static CompoundBinaryTag convertAndBack(CompoundBinaryTag value, Consumer<CompoundTag> acceptor) {
        if (value == null) return null;

        net.minecraft.nbt.CompoundTag converted = (net.minecraft.nbt.CompoundTag) Converter.convertTag(value);
        acceptor.accept(converted);

        return Converter.convertTag(converted);
    }

    private static CompoundBinaryTag upgradeTagEncodedVersions(CompoundBinaryTag value, MCDataType type, long encodedFrom, long encodedTo) {
        if (value == null) return null;
        return Converter.convertTag(upgradeTagEncodedVersionsNMS((CompoundTag) Converter.convertTag(value), type, encodedFrom, encodedTo));
    }

    private static CompoundTag upgradeTagEncodedVersionsNMS(CompoundTag value, MCDataType type, long encodedFrom, long encodedTo) {
        if (value == null) return null;

        final NBTMapType wrapped = new NBTMapType(value);
        final NBTMapType replaced = (NBTMapType)MCDataConverter.convertWithSubVersion(type, wrapped, encodedFrom, encodedTo);

        return replaced == null ? wrapped.getTag() : replaced.getTag();
    }

    @Override
    public CompoundBinaryTag convertChunkTo1_13(CompoundBinaryTag tag) {
        return convertChunk(tag, 1631);
    }

    @Override
    public CompoundBinaryTag convertChunk(CompoundBinaryTag globalTag, int to) {
        int version = globalTag.getInt("DataVersion");
        long encodedNewVersion = DataConverter.encodeVersions(to, Integer.MAX_VALUE);
        long encodedCurrentVersion = DataConverter.encodeVersions(Math.max(version, V99.VERSION), Integer.MAX_VALUE);

        return upgradeTagEncodedVersions(globalTag, MCTypeRegistry.CHUNK, encodedCurrentVersion, encodedNewVersion);
    }

    @Override
    public List<CompoundBinaryTag> convertEntities(List<CompoundBinaryTag> input, int from, int to) {
        List<CompoundBinaryTag> entities = new ArrayList<>(input.size());

        long encodedNewVersion = DataConverter.encodeVersions(to, Integer.MAX_VALUE);
        long encodedCurrentVersion = DataConverter.encodeVersions(Math.max(from, V99.VERSION), Integer.MAX_VALUE);

        for (CompoundBinaryTag upgradeEntity : input) {
            entities.add(
                    upgradeTagEncodedVersions(upgradeEntity, MCTypeRegistry.ENTITY, encodedCurrentVersion, encodedNewVersion)
            );
        }
        return entities;
    }

    @Override
    public List<CompoundBinaryTag> convertTileEntities(List<CompoundBinaryTag> input, int from, int to) {
        List<CompoundBinaryTag> blockEntities = new ArrayList<>(input.size());

        long encodedNewVersion = DataConverter.encodeVersions(to, Integer.MAX_VALUE);
        long encodedCurrentVersion = DataConverter.encodeVersions(Math.max(from, V99.VERSION), Integer.MAX_VALUE);

        for (CompoundBinaryTag upgradeEntity : input) {
            blockEntities.add(
                    upgradeTagEncodedVersions(upgradeEntity, MCTypeRegistry.TILE_ENTITY, encodedCurrentVersion, encodedNewVersion)
            );
        }
        return blockEntities;
    }

    @Override
    public ListBinaryTag convertBlockPalette(ListBinaryTag input, int from, int to) {
        long encodedNewVersion = DataConverter.encodeVersions(to, Integer.MAX_VALUE);
        long encodedCurrentVersion = DataConverter.encodeVersions(Math.max(from, V99.VERSION), Integer.MAX_VALUE);

        ListTag nbtList = (ListTag) Converter.convertTag(input);
        NBTListType listType = new NBTListType(nbtList);

        for (int i = 0, len = listType.size(); i < len; ++i) {
            final Object replace = MCDataConverter.convertWithSubVersion(MCTypeRegistry.BLOCK_STATE, listType.getMap(i),
                    encodedCurrentVersion, encodedNewVersion);
            if (replace != null) {
                listType.setGeneric(i, replace);
            }
        }

        return Converter.convertTag(listType.getTag());
    }

    @Override
    public int getServerVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }
}
