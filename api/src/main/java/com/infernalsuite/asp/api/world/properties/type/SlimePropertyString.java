package com.infernalsuite.asp.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.asp.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * A slime property of type integer
 */
public class SlimePropertyString extends SlimeProperty<String, StringBinaryTag> {

	public static SlimePropertyString create(final @NotNull String key, final String defaultValue) {
		Preconditions.checkNotNull(key, "Key cannot be null");
		return new SlimePropertyString(key, defaultValue);
	}

	public static SlimePropertyString create(final @NotNull String key, final String defaultValue, final @NotNull Function<String, Boolean> validator) {
		Preconditions.checkNotNull(key, "Key cannot be null");
		Preconditions.checkNotNull(validator, "Use SlimePropertyString#create(String, String) instead");
		return new SlimePropertyString(key, defaultValue, validator);
	}

	/**
	 * @deprecated Use {@link #create(String, String)} instead
	 */
	@Deprecated(forRemoval = true)
	public SlimePropertyString(String key, String defaultValue) {
		super(key, defaultValue);
	}

	/**
	 * @deprecated Use {@link #create(String, String, Function)} instead
	 */
	@Deprecated(forRemoval = true)
	public SlimePropertyString(String key, String defaultValue, Function<String, Boolean> validator) {
		super(key, defaultValue, validator);
	}

	@Override
	protected StringBinaryTag createTag(final String value) {
		return StringBinaryTag.stringBinaryTag(value);
	}

	@Override
	protected String readValue(final StringBinaryTag tag) {
		return tag.value();
	}

	@Override
	protected StringBinaryTag cast(BinaryTag rawTag) {
		return (StringBinaryTag) rawTag;
	}

}
