package com.infernalsuite.asp.api.world.properties;

import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A Property Map object.
 */
public class SlimePropertyMap {

    private final Map<String, BinaryTag> properties;

    public SlimePropertyMap() {
        this(new HashMap<>());
    }

    public SlimePropertyMap(final Map<String, BinaryTag> properties) {
        this.properties = properties;
    }

    /**
     * Return the current value of the given property
     *
     * @param property The slime property
     * @return The current value
     */
    public <T, Z extends BinaryTag> T getValue(final SlimeProperty<T, Z> property) {
        if (this.properties.containsKey(property.getKey())) {
            return property.readValue(property.cast(this.properties.get(property.getKey())));
        } else {
            return property.getDefaultValue();
        }
    }

    /**
     * Return the current value of the given property as an Optional
     * Instead of returning the default value if the property is not set, it returns an empty Optional
     *
     * @param property The slime property
     * @return An Optional containing the current value, or empty if not set
     */
    public <T, Z extends BinaryTag> Optional<T> getOptionalValue(final SlimeProperty<T, Z> property) {
        if (this.properties.containsKey(property.getKey())) {
            return Optional.of(property.readValue(property.cast(this.properties.get(property.getKey()))));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Return the properties (CompoundMap)
     *
     * @return The properties
     */
    public Map<String, BinaryTag> getProperties() {
        return this.properties;
    }

    /**
     * Update the value of the given property
     *
     * @param property The slime property
     * @param value    The new value
     * @throws IllegalArgumentException if the value fails validation.
     */
    public <T, Z extends BinaryTag> void setValue(final SlimeProperty<T, Z> property, final T value) {
        if (!property.applyValidator(value)) throw new IllegalArgumentException("'%s' is not a valid property value.".formatted(value));
        this.properties.put(property.getKey(), property.createTag(value));
    }

    /**
     * Copies all values from the specified {@link SlimePropertyMap}.
     * If the same property has different values on both maps, the one
     * on the provided map will be used.
     *
     * @param other A {@link SlimePropertyMap}.
     */
    public void merge(final SlimePropertyMap other) {
        this.properties.putAll(other.properties);
    }

    /**
     * Returns a {@link CompoundBinaryTag} containing every property set in this map.
     *
     * @return A {@link CompoundBinaryTag} with all the properties stored in this map.
     */
    public CompoundBinaryTag toCompound() {
        return CompoundBinaryTag.builder().put(this.properties).build();
    }

    public static SlimePropertyMap fromCompound(final CompoundBinaryTag tag) {
        final Map<String, BinaryTag> tags = new HashMap<>(tag.size());
        tag.forEach(entry -> tags.put(entry.getKey(), entry.getValue()));
        return new SlimePropertyMap(tags);
    }

    @SuppressWarnings("MethodDoesntCallSuperMethod")
    public SlimePropertyMap clone() {
        return new SlimePropertyMap(new HashMap<>(this.properties));
    }

    @Override
    public String toString() {
        return "SlimePropertyMap{" + properties + '}';
    }

}
