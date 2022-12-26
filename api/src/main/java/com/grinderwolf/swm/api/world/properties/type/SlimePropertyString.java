package com.grinderwolf.swm.api.world.properties.type;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.Tag;
import com.grinderwolf.swm.api.world.properties.SlimeProperty;

import java.util.function.Function;

/**
 * A slime property of type integer
 */
public class SlimePropertyString extends SlimeProperty<String> {

	public SlimePropertyString(String nbtName, String defaultValue) {
		super(nbtName, defaultValue);
	}

	public SlimePropertyString(String nbtName, String defaultValue, Function<String, Boolean> validator) {
		super(nbtName, defaultValue, validator);
	}

	@Override
	protected void writeValue(CompoundMap compound, String value) {
		compound.put(getNbtName(), new StringTag(getNbtName(), value));
	}

	@Override
	protected String readValue(Tag<?> compoundTag) {
		return compoundTag.getAsStringTag()
			.map(Tag::getValue)
			.orElse(getDefaultValue());
	}
}
