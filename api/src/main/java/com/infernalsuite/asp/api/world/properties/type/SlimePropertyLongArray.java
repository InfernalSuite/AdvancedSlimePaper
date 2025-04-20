package com.infernalsuite.asp.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.asp.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class SlimePropertyLongArray extends SlimeProperty<long[], LongArrayBinaryTag> {

    public static SlimePropertyLongArray create(final @NotNull String key, final long[] defaultValue) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        return new SlimePropertyLongArray(key, defaultValue);
    }

    public static SlimePropertyLongArray create(final @NotNull String key, final long[] defaultValue, final @NotNull Function<long[], Boolean> validator) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(validator, "Use SlimePropertyLongArray#create(String, long[]) instead");
        return new SlimePropertyLongArray(key, defaultValue, validator);
    }

    private SlimePropertyLongArray(String key, long[] defaultValue) {
        super(key, defaultValue);
    }

    private SlimePropertyLongArray(String key, long[] defaultValue, Function<long[], Boolean> validator) {
        super(key, defaultValue, validator);
    }

    @Override
    protected LongArrayBinaryTag createTag(final long[] value) {
        return LongArrayBinaryTag.longArrayBinaryTag(value);
    }

    @Override
    protected long[] readValue(final LongArrayBinaryTag tag) {
        return tag.value();
    }

    @Override
    protected LongArrayBinaryTag cast(BinaryTag rawTag) {
        return (LongArrayBinaryTag) rawTag;
    }

}
