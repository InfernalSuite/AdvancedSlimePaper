package com.infernalsuite.asp.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.asp.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * A slime property of type float
 */
public class SlimePropertyFloat extends SlimeProperty<Float, FloatBinaryTag> {

    public static SlimePropertyFloat create(final @NotNull String key, final float defaultValue) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        return new SlimePropertyFloat(key, defaultValue);
    }

    public static SlimePropertyFloat create(final @NotNull String key, final float defaultValue, final @NotNull Function<Float, Boolean> validator) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(validator, "Use SlimePropertyFloat#create(String, float) instead");
        return new SlimePropertyFloat(key, defaultValue, validator);
    }

    /**
     * @deprecated use {@link #create(String, float)} instead
     */
    @Deprecated(forRemoval = true)
    public SlimePropertyFloat(String key, Float defaultValue) {
        super(key, defaultValue);
    }

    /**
     * @deprecated use {@link #create(String, float, Function)} instead
     */
    @Deprecated(forRemoval = true)
    public SlimePropertyFloat(String key, Float defaultValue, Function<Float, Boolean> validator) {
        super(key, defaultValue, validator);
    }

    @Override
    protected FloatBinaryTag createTag(final Float value) {
        return FloatBinaryTag.floatBinaryTag(value);
    }

    @Override
    protected Float readValue(final FloatBinaryTag tag) {
        return tag.value();
    }

    @Override
    protected FloatBinaryTag cast(BinaryTag rawTag) {
        return (FloatBinaryTag) rawTag;
    }

}
