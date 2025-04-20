package com.infernalsuite.asp.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.asp.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class SlimePropertyByte extends SlimeProperty<Byte, ByteBinaryTag> {

    public static SlimePropertyByte create(final @NotNull String key, final byte defaultValue) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        return new SlimePropertyByte(key, defaultValue);
    }

    public static SlimePropertyByte create(final @NotNull String key, final byte defaultValue, final @NotNull Function<Byte, Boolean> validator) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(validator, "Use SlimePropertyByte#create(String, byte) instead");
        return new SlimePropertyByte(key, defaultValue, validator);
    }

    private SlimePropertyByte(String key, Byte defaultValue) {
        super(key, defaultValue);
    }

    private SlimePropertyByte(String key, Byte defaultValue, Function<Byte, Boolean> validator) {
        super(key, defaultValue, validator);
    }

    @Override
    protected ByteBinaryTag createTag(final Byte value) {
        return ByteBinaryTag.byteBinaryTag(value);
    }

    @Override
    protected Byte readValue(final ByteBinaryTag tag) {
        return tag.value();
    }

    @Override
    protected ByteBinaryTag cast(BinaryTag rawTag) {
        return (ByteBinaryTag) rawTag;
    }

}
