package com.infernalsuite.asp.data;

import ca.spottedleaf.moonrise.common.PlatformHooks;
import com.infernalsuite.asp.Converter;
import com.infernalsuite.asp.api.SlimeDataConverter;
import com.infernalsuite.asp.api.world.SlimeChunk;
import com.infernalsuite.asp.api.world.SlimeChunkSection;
import com.infernalsuite.asp.api.world.SlimeWorld;
import com.infernalsuite.asp.level.chunk.SlimeChunkConverter;
import com.infernalsuite.asp.serialization.SlimeWorldReader;
import com.infernalsuite.asp.skeleton.SkeletonSlimeWorld;
import com.infernalsuite.asp.skeleton.SlimeChunkSectionSkeleton;
import com.infernalsuite.asp.skeleton.SlimeChunkSkeleton;
import com.infernalsuite.asp.util.Util;
import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.fixes.References;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Experimental data converter that uses the paper hooks and dfu directly instead of directly using DataConverter to make version
 * upgrades less reliant on it. Very experimental and only used when necessary for now. SimpleDataFixerConverter is and remains preferred for now.
 * <p>
 * This could in theory replace SimpleDataFixerConverter in the future, once convertList isn't directly reliant on DFU anymore.
 * PlatformHooks already use DataConverter whenever possible, but convertList does not.
 */
public class DFUConverter implements SlimeWorldReader<SlimeWorld>, SlimeDataConverter {

    private static final Logger LOGGER = LogManager.getLogger();

    @Override
    public SlimeWorld readFromData(SlimeWorld data) {
        int newVersion = SharedConstants.getCurrentVersion().dataVersion().version();
        int currentVersion = data.getDataVersion();
        // Already fixed
        if (currentVersion == newVersion) {
            return data;
        }
        DataFixer dataFixer = MinecraftServer.getServer().getFixerUpper();

        Long2ObjectMap<SlimeChunk> chunks = new Long2ObjectOpenHashMap<>();
        for (SlimeChunk chunk : data.getChunkStorage()) {
            List<CompoundBinaryTag> entities = new ArrayList<>();
            List<CompoundBinaryTag> blockEntities = new ArrayList<>();
            for (CompoundBinaryTag upgradeEntity : chunk.getTileEntities()) {
                blockEntities.add(
                        convertAndBack(upgradeEntity, (tag) -> PlatformHooks.get().convertNBT(References.BLOCK_ENTITY, dataFixer, tag, currentVersion, newVersion))
                );
            }
            for (CompoundBinaryTag upgradeEntity : chunk.getEntities()) {
                entities.add(
                        convertAndBack(upgradeEntity, (tag) -> PlatformHooks.get().convertNBT(References.ENTITY, dataFixer, tag, currentVersion, newVersion))
                );
            }
            long chunkPos = Util.chunkPosition(chunk.getX(), chunk.getZ());

            SlimeChunkSection[] sections = new SlimeChunkSection[chunk.getSections().length];
            for (int i = 0; i < sections.length; i++) {
                SlimeChunkSection dataSection = chunk.getSections()[i];
                if (dataSection == null) continue;

                CompoundBinaryTag blockStateTag = convertAndBackSameRef(dataSection.getBlockStatesTag(), (tag) -> {
                    convertList(References.BLOCK_STATE, dataFixer, tag, "palette", currentVersion, newVersion);
                });

                CompoundBinaryTag biomeTag = convertAndBackSameRef(dataSection.getBiomeTag(), (tag) -> {
                    convertList(References.BIOME, dataFixer, tag, "palette", currentVersion, newVersion);
                });

                sections[i] = new SlimeChunkSectionSkeleton(
                        blockStateTag,
                        biomeTag,
                        dataSection.getBlockLight(),
                        dataSection.getSkyLight()
                );
            }

            CompoundBinaryTag newPoi = chunk.getPoiChunkSections() != null ? convertPoiSections(chunk.getPoiChunkSections(), dataFixer, currentVersion, newVersion) : null;

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

    private void convertList(DSL.TypeReference ref, DataFixer dataFixer, CompoundTag tag, String listName, int currentVersion, int newVersion) {
        Optional<ListTag> list = tag.getList(listName);
        if(list.isEmpty()) {
            LOGGER.debug("Could not find {} in {}", listName, tag);
            return;
        }
        ListTag tags = list.get();
        ListTag newTags = new ListTag();

        for (Tag current : tags) {
            //Unfortunately there is only a paper hook for compound tags :/
            newTags.add(
                    dataFixer.update(
                            ref, new Dynamic<>(NbtOps.INSTANCE, current), currentVersion, newVersion
                    ).getValue()
            );
        }
        tag.put(listName, newTags);
    }

    private CompoundBinaryTag convertPoiSections(CompoundBinaryTag poiChunkSections, DataFixer dataFixer, int currentVersion, int newVersion) {
        CompoundTag poiChunk = SlimeChunkConverter.createPoiChunkFromSlimeSections(poiChunkSections, currentVersion);
        CompoundTag convertedNBT = PlatformHooks.get().convertNBT(References.POI_CHUNK, dataFixer, poiChunk, currentVersion, newVersion);
        return SlimeChunkConverter.getSlimeSectionsFromPoiCompound(convertedNBT);
    }

    @Override
    public SlimeWorld applyDataFixers(SlimeWorld world) {
        return readFromData(world);
    }

    private static CompoundBinaryTag convertAndBackSameRef(CompoundBinaryTag value, Consumer<CompoundTag> acceptor) {
        if (value == null) return null;

        CompoundTag converted = (CompoundTag) Converter.convertTag(value);
        acceptor.accept(converted);

        return Converter.convertTag(converted);
    }

    private static CompoundBinaryTag convertAndBack(CompoundBinaryTag value, Function<CompoundTag, CompoundTag> acceptor) {
        if (value == null) return null;

        CompoundTag converted = (CompoundTag) Converter.convertTag(value);
        return Converter.convertTag(
                acceptor.apply(converted)
        );
    }

    @Override
    public CompoundBinaryTag convertChunkTo1_13(CompoundBinaryTag tag) {
        return convertChunk(tag, 1631);
    }

    @Override
    public CompoundBinaryTag convertChunk(CompoundBinaryTag globalTag, int to) {
        CompoundTag nmsTag = (CompoundTag) Converter.convertTag(globalTag);

        int version = nmsTag.getInt("DataVersion").orElseThrow();
        DataFixer dataFixer = MinecraftServer.getServer().getFixerUpper();

        return Converter.convertTag(PlatformHooks.get().convertNBT(References.CHUNK, dataFixer, nmsTag, version, to));
    }

    @Override
    public List<CompoundBinaryTag> convertEntities(List<CompoundBinaryTag> input, int from, int to) {
        List<CompoundBinaryTag> entities = new ArrayList<>(input.size());
        DataFixer dataFixer = MinecraftServer.getServer().getFixerUpper();

        for (CompoundBinaryTag upgradeEntity : input) {
            entities.add(
                    convertAndBack(upgradeEntity, (tag) -> PlatformHooks.get().convertNBT(References.CHUNK, dataFixer, tag, from, to))
            );
        }
        return entities;
    }

    @Override
    public List<CompoundBinaryTag> convertTileEntities(List<CompoundBinaryTag> input, int from, int to) {
        List<CompoundBinaryTag> blockEntities = new ArrayList<>(input.size());
        DataFixer dataFixer = MinecraftServer.getServer().getFixerUpper();

        for (CompoundBinaryTag upgradeEntity : input) {
            blockEntities.add(
                    convertAndBack(upgradeEntity, (tag) -> PlatformHooks.get().convertNBT(References.BLOCK_ENTITY, dataFixer, tag, from, to))
            );
        }
        return blockEntities;
    }

    @Override
    public ListBinaryTag convertBlockPalette(ListBinaryTag input, int from, int to) {
        DataFixer dataFixer = MinecraftServer.getServer().getFixerUpper();

        ListTag nbtList = (ListTag) Converter.convertTag(input);

        for (int i = 0, len = nbtList.size(); i < len; ++i) {

            nbtList.set(i, dataFixer.update(
                    References.BLOCK_STATE, new Dynamic<>(NbtOps.INSTANCE, nbtList.get(i)), from, to
            ).getValue());
        }

        return Converter.convertTag(nbtList);
    }

    @Override
    public int getServerVersion() {
        return SharedConstants.getCurrentVersion().dataVersion().version();
    }
}
