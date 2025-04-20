package com.infernalsuite.asp.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.asp.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class SlimePropertyIntArray extends SlimeProperty<int[], IntArrayBinaryTag> {

    public static SlimePropertyIntArray create(final @NotNull String key, final int[] defaultValue) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        return new SlimePropertyIntArray(key, defaultValue);
    }

    public static SlimePropertyIntArray create(final @NotNull String key, final int[] defaultValue, final @NotNull Function<int[], Boolean> validator) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(validator, "Use SlimePropertyIntArray#create(String, int[]) instead");
        return new SlimePropertyIntArray(key, defaultValue, validator);
    }

    private SlimePropertyIntArray(String key, int[] defaultValue) {
        super(key, defaultValue);
    }

    private SlimePropertyIntArray(String key, int[] defaultValue, Function<int[], Boolean> validator) {
        super(key, defaultValue, validator);
    }

    @Override
    protected IntArrayBinaryTag createTag(final int[] value) {
        return IntArrayBinaryTag.intArrayBinaryTag(value);
    }

    @Override
    protected int[] readValue(final IntArrayBinaryTag tag) {
        return tag.value();
    }

    @Override
    protected IntArrayBinaryTag cast(BinaryTag rawTag) {
        return (IntArrayBinaryTag) rawTag;
    }

}
