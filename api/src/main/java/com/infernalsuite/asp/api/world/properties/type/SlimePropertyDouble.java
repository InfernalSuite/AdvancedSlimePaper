package com.infernalsuite.asp.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.asp.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public class SlimePropertyDouble extends SlimeProperty<Double, DoubleBinaryTag> {

    public static SlimePropertyDouble create(final @NotNull String key, final double defaultValue) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        return new SlimePropertyDouble(key, defaultValue);
    }

    public static SlimePropertyDouble create(final @NotNull String key, final double defaultValue, final @NotNull Function<Double, Boolean> validator) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(validator, "Use SlimePropertyDouble#create(String, double) instead");
        return new SlimePropertyDouble(key, defaultValue, validator);
    }

    private SlimePropertyDouble(String key, Double defaultValue) {
        super(key, defaultValue);
    }

    private SlimePropertyDouble(String key, Double defaultValue, Function<Double, Boolean> validator) {
        super(key, defaultValue, validator);
    }

    @Override
    protected DoubleBinaryTag createTag(final Double value) {
        return DoubleBinaryTag.doubleBinaryTag(value);
    }

    @Override
    protected Double readValue(final DoubleBinaryTag tag) {
        return tag.value();
    }

    @Override
    protected DoubleBinaryTag cast(BinaryTag rawTag) {
        return (DoubleBinaryTag) rawTag;
    }
}
