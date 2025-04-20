package com.infernalsuite.asp.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.asp.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * A slime property of type boolean
 */
public class SlimePropertyBoolean extends SlimeProperty<Boolean, ByteBinaryTag> {

	public static SlimePropertyBoolean create(final @NotNull String key, final boolean defaultValue) {
		Preconditions.checkNotNull(key, "Key cannot be null");
		return new SlimePropertyBoolean(key, defaultValue);
	}

	public static SlimePropertyBoolean create(final @NotNull String key, final boolean defaultValue, final @NotNull Function<Boolean, Boolean> validator) {
		Preconditions.checkNotNull(key, "Key cannot be null");
		Preconditions.checkNotNull(validator, "Use SlimePropertyBoolean#create(String, boolean) instead");
		return new SlimePropertyBoolean(key, defaultValue, validator);
	}

	/**
	 * @deprecated Use {@link #create(String, boolean)} instead
	 */
	@Deprecated(forRemoval = true)
	public SlimePropertyBoolean(String key, Boolean defaultValue) {
		super(key, defaultValue);
	}

	/**
	 * @deprecated Use {@link #create(String, boolean, Function)} instead
	 */
	@Deprecated(forRemoval = true)
	public SlimePropertyBoolean(String key, Boolean defaultValue, Function<Boolean, Boolean> validator) {
		super(key, defaultValue, validator);
	}

	@Override
	protected ByteBinaryTag createTag(final Boolean value) {
		return value ? ByteBinaryTag.ONE : ByteBinaryTag.ZERO;
	}

	@Override
	protected Boolean readValue(final ByteBinaryTag tag) {
		return tag.value() == 1;
	}

	@Override
	protected ByteBinaryTag cast(BinaryTag rawTag) {
		return (ByteBinaryTag) rawTag;
	}
}
