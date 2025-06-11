package com.infernalsuite.asp;

import com.infernalsuite.asp.api.utils.NibbleArray;
import net.kyori.adventure.nbt.*;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.EndTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.chunk.DataLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Converter {

    private static final Logger LOGGER = LogManager.getLogger("ASP NBT Converter");

    static DataLayer convertArray(NibbleArray array) {
        return new DataLayer(array.getBacking());
    }

    public static NibbleArray convertArray(DataLayer array) {
        if(array == null) {
            return null;
        }

        return new NibbleArray(array.getData());
    }

    public static <T extends BinaryTag> Tag convertTag(T tag) {
        try {
            return switch (tag.type().id()) {
                case Tag.TAG_END -> EndTag.INSTANCE;
                case Tag.TAG_BYTE -> ByteTag.valueOf(((ByteBinaryTag) tag).value());
                case Tag.TAG_SHORT -> ShortTag.valueOf(((ShortBinaryTag) tag).value());
                case Tag.TAG_INT -> IntTag.valueOf(((IntBinaryTag) tag).value());
                case Tag.TAG_LONG -> LongTag.valueOf(((LongBinaryTag) tag).value());
                case Tag.TAG_FLOAT -> FloatTag.valueOf(((FloatBinaryTag) tag).value());
                case Tag.TAG_DOUBLE -> DoubleTag.valueOf(((DoubleBinaryTag) tag).value());
                case Tag.TAG_BYTE_ARRAY -> new ByteArrayTag(((ByteArrayBinaryTag) tag).value());
                case Tag.TAG_STRING -> StringTag.valueOf(((StringBinaryTag) tag).value());
                case Tag.TAG_LIST -> {
                    ListTag list = new ListTag();
                    for (BinaryTag entry : ((ListBinaryTag) tag)) list.add(convertTag(entry));
                    yield list;
                }
                case Tag.TAG_COMPOUND -> {
                    CompoundTag compound = new CompoundTag();
                    ((CompoundBinaryTag) tag).forEach(entry -> compound.put(entry.getKey(), convertTag(entry.getValue())));
                    yield compound;
                }
                case Tag.TAG_INT_ARRAY -> new IntArrayTag(((IntArrayBinaryTag) tag).value());
                case Tag.TAG_LONG_ARRAY -> new LongArrayTag(((LongArrayBinaryTag) tag).value());
                default -> throw new IllegalArgumentException("Invalid tag type " + tag.type().id());
            };
        } catch (final Exception e) {
            CompoundBinaryTag exceptionTag = CompoundBinaryTag.builder().put("failing_tag", tag).build();
            String tagString;
            try {
                tagString = TagStringIO.get().asString(exceptionTag);
            } catch (final IOException ioEx) {
                LOGGER.error("Error while trying to convert exception tag to string", ioEx);
                tagString = "UNAVAILABLE";
            }
            LOGGER.error("Failed to convert NBT object: {}", tagString);
            throw e;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends BinaryTag> T convertTag(Tag base) {
        return switch (base.getId()) {
            case Tag.TAG_END -> (T) EndBinaryTag.endBinaryTag();
            case Tag.TAG_BYTE -> (T) ByteBinaryTag.byteBinaryTag(((ByteTag) base).byteValue());
            case Tag.TAG_SHORT -> (T) ShortBinaryTag.shortBinaryTag(((ShortTag) base).shortValue());
            case Tag.TAG_INT -> (T) IntBinaryTag.intBinaryTag(((IntTag) base).intValue());
            case Tag.TAG_LONG -> (T) LongBinaryTag.longBinaryTag(((LongTag) base).longValue());
            case Tag.TAG_FLOAT -> (T) FloatBinaryTag.floatBinaryTag(((FloatTag) base).floatValue());
            case Tag.TAG_DOUBLE -> (T) DoubleBinaryTag.doubleBinaryTag(((DoubleTag) base).doubleValue());
            case Tag.TAG_BYTE_ARRAY -> (T) ByteArrayBinaryTag.byteArrayBinaryTag(((ByteArrayTag) base).getAsByteArray());
            case Tag.TAG_STRING -> (T) StringBinaryTag.stringBinaryTag(base.asString().orElseThrow()); //TODO(david): Figure out what to do with optional?
            case Tag.TAG_LIST -> {
                ListTag originalList = ((ListTag) base);
                if(originalList.isEmpty()) {
                    yield (T) ListBinaryTag.empty();
                }
                List<BinaryTag> list = new ArrayList<>(originalList.size());

                BinaryTagType<?> tagType = null;
                for (Tag entry : originalList) {
                    BinaryTag converted = convertTag(entry);

                    if(tagType != null && !converted.type().equals(tagType)) {
                        tagType = BinaryTagTypes.LIST_WILDCARD;
                    } else if(tagType == null) {
                        tagType = converted.type();
                    }

                    list.add(converted);
                }
                yield (T) ListBinaryTag.listBinaryTag(tagType, list);
            }
            case Tag.TAG_COMPOUND -> {
                CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();
                CompoundTag originalCompound = ((CompoundTag) base);
                for (String key : originalCompound.keySet()) builder.put(key, convertTag(Objects.requireNonNull(originalCompound.get(key))));
                yield (T) builder.build();
            }
            case Tag.TAG_INT_ARRAY -> (T) IntArrayBinaryTag.intArrayBinaryTag(((IntArrayTag) base).getAsIntArray());
            case Tag.TAG_LONG_ARRAY -> (T) LongArrayBinaryTag.longArrayBinaryTag(((LongArrayTag) base).getAsLongArray());
            default -> throw new IllegalArgumentException("Invalid tag type " + base.getId());
        };
    }
}
