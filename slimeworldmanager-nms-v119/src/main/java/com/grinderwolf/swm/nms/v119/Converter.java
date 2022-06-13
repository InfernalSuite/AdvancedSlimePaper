package com.grinderwolf.swm.nms.v119;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.TagType;
import com.grinderwolf.swm.api.utils.NibbleArray;
import net.minecraft.nbt.*;
import net.minecraft.world.level.chunk.DataLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class Converter {

    private static final Logger LOGGER = LogManager.getLogger("SWM Converter");

    static DataLayer convertArray(NibbleArray array) {
        return new DataLayer(array.getBacking());
    }

    static NibbleArray convertArray(DataLayer array) {
        if (array == null) {
            return null;
        }

        return new NibbleArray(array.getData());
    }

    static Tag convertTag(com.flowpowered.nbt.Tag tag) {
        try {
            switch (tag.getType()) {
                case TAG_BYTE:
                    return ByteTag.valueOf(((com.flowpowered.nbt.ByteTag) tag).getValue());
                case TAG_SHORT:
                    return ShortTag.valueOf(((com.flowpowered.nbt.ShortTag) tag).getValue());
                case TAG_INT:
                    return IntTag.valueOf(((com.flowpowered.nbt.IntTag) tag).getValue());
                case TAG_LONG:
                    return LongTag.valueOf(((com.flowpowered.nbt.LongTag) tag).getValue());
                case TAG_FLOAT:
                    return FloatTag.valueOf(((com.flowpowered.nbt.FloatTag) tag).getValue());
                case TAG_DOUBLE:
                    return DoubleTag.valueOf(((com.flowpowered.nbt.DoubleTag) tag).getValue());
                case TAG_BYTE_ARRAY:
                    return new ByteArrayTag(((com.flowpowered.nbt.ByteArrayTag) tag).getValue());
                case TAG_STRING:
                    return StringTag.valueOf(((com.flowpowered.nbt.StringTag) tag).getValue());
                case TAG_LIST:
                    ListTag list = new ListTag();
                    ((com.flowpowered.nbt.ListTag<?>) tag).getValue().stream().map(Converter::convertTag).forEach(list::add);

                    return list;
                case TAG_COMPOUND:
                    CompoundTag compound = new CompoundTag();

                    ((com.flowpowered.nbt.CompoundTag) tag).getValue().forEach((key, value) -> compound.put(key, convertTag(value)));
                    return compound;
                case TAG_INT_ARRAY:
                    return new IntArrayTag(((com.flowpowered.nbt.IntArrayTag) tag).getValue());
                case TAG_LONG_ARRAY:
                    return new LongArrayTag(((com.flowpowered.nbt.LongArrayTag) tag).getValue());
                default:
                    throw new IllegalArgumentException("Invalid tag type " + tag.getType().name());
            }
        } catch (Exception ex) {
            LOGGER.error("Failed to convert NBT object:");
            LOGGER.error(tag.toString());

            throw ex;
        }
    }

    static com.flowpowered.nbt.Tag convertTag(String name, Tag base) {
        switch (base.getId()) {
            case Tag.TAG_BYTE:
                return new com.flowpowered.nbt.ByteTag(name, ((ByteTag) base).getAsByte());
            case Tag.TAG_SHORT:
                return new com.flowpowered.nbt.ShortTag(name, ((ShortTag) base).getAsShort());
            case Tag.TAG_INT:
                return new com.flowpowered.nbt.IntTag(name, ((IntTag) base).getAsInt());
            case Tag.TAG_LONG:
                return new com.flowpowered.nbt.LongTag(name, ((LongTag) base).getAsLong());
            case Tag.TAG_FLOAT:
                return new com.flowpowered.nbt.FloatTag(name, ((FloatTag) base).getAsFloat());
            case Tag.TAG_DOUBLE:
                return new com.flowpowered.nbt.DoubleTag(name, ((DoubleTag) base).getAsDouble());
            case Tag.TAG_BYTE_ARRAY:
                return new com.flowpowered.nbt.ByteArrayTag(name, ((ByteArrayTag) base).getAsByteArray());
            case Tag.TAG_STRING:
                return new com.flowpowered.nbt.StringTag(name, ((StringTag) base).getAsString());
            case Tag.TAG_LIST:
                List<com.flowpowered.nbt.Tag> list = new ArrayList<>();
                ListTag originalList = ((ListTag) base);

                for (Tag entry : originalList) {
                    list.add(convertTag("", entry));
                }

                return new com.flowpowered.nbt.ListTag(name, TagType.getById(originalList.getElementType()), list);
            case Tag.TAG_COMPOUND:
                CompoundTag originalCompound = ((CompoundTag) base);
                com.flowpowered.nbt.CompoundTag compound = new com.flowpowered.nbt.CompoundTag(name, new CompoundMap());

                for (String key : originalCompound.getAllKeys()) {
                    compound.getValue().put(key, convertTag(key, originalCompound.get(key)));
                }

                return compound;
            case Tag.TAG_INT_ARRAY:
                return new com.flowpowered.nbt.IntArrayTag(name, ((IntArrayTag) base).getAsIntArray());
            case Tag.TAG_LONG_ARRAY:
                return new com.flowpowered.nbt.LongArrayTag(name, ((LongArrayTag) base).getAsLongArray());
            default:
                throw new IllegalArgumentException("Invalid tag type " + base.getId());
        }
    }

}