package com.infernalsuite.asp.api.world.properties.type;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.FloatTag;
import com.flowpowered.nbt.Tag;

import java.util.function.Function;

/**
 * A slime property of type float
 */
public class SlimePropertyFloat extends com.infernalsuite.asp.api.world.properties.SlimeProperty<Float> {

    public SlimePropertyFloat(String nbtName, Float defaultValue, Function<Float, Boolean> validator) {
        super(nbtName, defaultValue, validator);
    }

    public SlimePropertyFloat(String nbtName, Float defaultValue) {
        super(nbtName, defaultValue);
    }

    @Override
    protected void writeValue(CompoundMap compound, Float value) {
        compound.put(getNbtName(), new FloatTag(getNbtName(), value));
    }

    @Override
    protected Float readValue(Tag<?> compoundTag) {
        return compoundTag.getAsFloatTag()
                .map(Tag::getValue)
                .orElse(getDefaultValue());
    }
}
