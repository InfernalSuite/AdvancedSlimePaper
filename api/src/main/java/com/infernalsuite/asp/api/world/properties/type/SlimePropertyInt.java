package com.infernalsuite.asp.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.asp.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * A slime property of type integer
 */
public class SlimePropertyInt extends SlimeProperty<Integer, IntBinaryTag> {

	public static SlimePropertyInt create(final @NotNull String key, final int defaultValue) {
		Preconditions.checkNotNull(key, "Key cannot be null");
		return new SlimePropertyInt(key, defaultValue);
	}

	public static SlimePropertyInt create(final @NotNull String key, final int defaultValue, final @NotNull Function<Integer, Boolean> validator) {
		Preconditions.checkNotNull(key, "Key cannot be null");
		Preconditions.checkNotNull(validator, "Use SlimePropertyInt#create(String, int) instead");
		return new SlimePropertyInt(key, defaultValue, validator);
	}

	/**
	 * @deprecated Use {@link #create(String, int)} instead
	 */
	@Deprecated(forRemoval = true)
	public SlimePropertyInt(String key, Integer defaultValue) {
		super(key, defaultValue);
	}

	/**
	 * @deprecated Use {@link #create(String, int, Function)} instead
	 */
	@Deprecated(forRemoval = true)
	public SlimePropertyInt(String key, Integer defaultValue, Function<Integer, Boolean> validator) {
		super(key, defaultValue, validator);
	}

	@Override
	protected IntBinaryTag createTag(final Integer value) {
		return IntBinaryTag.intBinaryTag(value);
	}

	@Override
	protected Integer readValue(final IntBinaryTag tag) {
		return tag.value();
	}

	@Override
	protected IntBinaryTag cast(BinaryTag rawTag) {
		return (IntBinaryTag) rawTag;
	}

}
