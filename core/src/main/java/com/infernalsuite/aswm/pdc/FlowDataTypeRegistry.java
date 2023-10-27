package com.infernalsuite.aswm.pdc;

import com.flowpowered.nbt.*;
import com.google.common.base.Preconditions;
import com.google.common.primitives.Primitives;
import com.infernalsuite.aswm.api.SlimeNMSBridge;
import net.kyori.adventure.util.Services;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

public class FlowDataTypeRegistry {

    public static final FlowDataTypeRegistry DEFAULT = new FlowDataTypeRegistry();
    private final Map<Class<?>, TagAdapter<?, ?>> adapters = new ConcurrentHashMap<>();

    private <T, Z extends Tag<?>> TagAdapter<T, Z> createAdapter(Class<T> primitiveType, Class<Z> nbtBaseType, BiFunction<String, T, Z> builder, Function<Z, T> extractor) {
        return new TagAdapter<>(primitiveType, nbtBaseType, builder, extractor);
    }

    private <T, Z extends Tag<?>> TagAdapter<T, Z> obtainAdapter(Class<T> type) {
        // Should be safe
        //noinspection unchecked
        return (TagAdapter<T, Z>) adapters.computeIfAbsent(type, this::createAdapter);
    }

    /**
     * Creates a TagAdapter object based on the given type.
     * <br>
     * <b>Unlike CraftBukkit, this implementation does not allow complex tags</b>, as it would require to cast them to CraftPersistentDataContainer which is not available from this context.
     *
     * @param type The class representing the type to be converted.
     * @return A TagAdapter object that can convert the given type to the corresponding Tag implementation.
     * @throws IllegalArgumentException if a valid TagAdapter implementation cannot be found for the requested type.
     */
    private <T> TagAdapter<?, ?> createAdapter(Class<T> type) throws IllegalArgumentException {
        if (!Primitives.isWrapperType(type)) {
            type = Primitives.wrap(type); //Make sure we will always "switch" over the wrapper types
        }
        // This would really make use of pattern matching in JDK 21 :(

        // Convert Byte to ByteTag
        if (Objects.equals(Byte.class, type)) {
            return createAdapter(Byte.class, ByteTag.class, ByteTag::new, ByteTag::getValue);
        }
        // Convert Short to ShortTag
        if (Objects.equals(Short.class, type)) {
            return createAdapter(Short.class, ShortTag.class, ShortTag::new, ShortTag::getValue);
        }
        // Convert Integer to IntTag
        if (Objects.equals(Integer.class, type)) {
            return createAdapter(Integer.class, IntTag.class, IntTag::new, IntTag::getValue);
        }
        // Convert Long to LongTag
        if (Objects.equals(Long.class, type)) {
            return createAdapter(Long.class, LongTag.class, LongTag::new, LongTag::getValue);
        }
        // Convert Float to FloatTag
        if (Objects.equals(Float.class, type)) {
            return createAdapter(Float.class, FloatTag.class, FloatTag::new, FloatTag::getValue);
        }
        // Convert Double to DoubleTag
        if (Objects.equals(Double.class, type)) {
            return createAdapter(Double.class, DoubleTag.class, DoubleTag::new, DoubleTag::getValue);
        }

        // Convert String to StringTag
        if (Objects.equals(String.class, type)) {
            return createAdapter(String.class, StringTag.class, StringTag::new, StringTag::getValue);
        }

        // Convert byte[] to ByteArrayTag
        if (Objects.equals(byte[].class, type)) {
            return createAdapter(byte[].class, ByteArrayTag.class, ByteArrayTag::new, ByteArrayTag::getValue);
        }
        // Convert int[] to IntArrayTag
        if (Objects.equals(int[].class, type)) {
            return createAdapter(int[].class, IntArrayTag.class, IntArrayTag::new, IntArrayTag::getValue);
        }
        // Convert long[] to LongArrayTag
        if (Objects.equals(long[].class, type)) {
            return createAdapter(long[].class, LongArrayTag.class, LongArrayTag::new, LongArrayTag::getValue);
        }
        // Convert short[] to ShortArrayTag
        if (Objects.equals(short[].class, type)) {
            return createAdapter(short[].class, ShortArrayTag.class, ShortArrayTag::new, ShortArrayTag::getValue);
        }

        if (Objects.equals(PersistentDataContainer.class, type)) {
            return createAdapter(PersistentDataContainer.class, CompoundTag.class, this::extractPDCIntoFlowNBT, this::extractFlowNBTIntoPDC);
        }

        if (Objects.equals(PersistentDataContainer[].class, type)) {
            return createAdapter(PersistentDataContainer[].class, ListTag.class, (key, value) -> {
                var list = new ArrayList<CompoundTag>();

                for (PersistentDataContainer pdc : value) {
                    list.add(extractPDCIntoFlowNBT(key, pdc));
                }

                return new ListTag<>(key, TagType.TAG_COMPOUND, list);
            }, tag -> {
                @SuppressWarnings("unchecked") var casted = (ListTag<CompoundTag>) tag;
                var list = casted.getValue();
                var resArr = new PersistentDataContainer[list.size()];

                for (int i = 0; i < list.size(); i++) {
                    resArr[i] = extractFlowNBTIntoPDC(list.get(i));
                }

                return resArr;
            });
        }

        throw new IllegalArgumentException("Could not find a valid TagAdapter implementation for the requested type " + type.getSimpleName());
    }

    private PersistentDataContainer extractFlowNBTIntoPDC(CompoundTag compound) {
        var optBridge = Services.service(SlimeNMSBridge.class);
        if (optBridge.isPresent()) {
            var bridge = optBridge.get();
            return bridge.extractCompoundMapIntoCraftPDC(compound.getValue());
        } else {
            // Fall back to FlowPersistentDataContainer
            var container = new FlowPersistentDataContainer(new CompoundTag("root", new CompoundMap()), this);
            container.getRoot().getValue().putAll(compound.getValue());
            return container;
        }
    }

    private CompoundTag extractPDCIntoFlowNBT(String key, PersistentDataContainer pdc) {
        var map = new CompoundMap();

        if (pdc instanceof FlowPersistentDataContainer container) {
            map.putAll(container.getRoot().getValue());
        } else {
            Services.service(SlimeNMSBridge.class).orElseThrow().extractCraftPDC(pdc, map);
        }

        return new CompoundTag(key, map);
    }

    /**
     * Wraps the passed value into a tag instance.
     *
     * @param type  the type of the passed value
     * @param value the value to be stored in the tag
     * @param key   the key to store the value under
     * @param <T>   the generic type of the value
     * @return the created tag instance
     * @throws IllegalArgumentException if no suitable tag type adapter for this type was found
     */
    public <T> Tag<?> wrap(String key, Class<T> type, T value) throws IllegalArgumentException {
        return obtainAdapter(type)
                .build(key, value);
    }

    /**
     * Returns if the tag instance matches the provided primitive type.
     *
     * @param type the type of the primitive value
     * @param base the base instance to check
     * @param <T>  the generic type of the type
     * @return if the base stores values of the primitive type passed
     * @throws IllegalArgumentException if no suitable tag type adapter for this
     *                                  type was found
     */
    public <T> boolean isInstanceOf(Class<T> type, Tag<?> base) {
        return obtainAdapter(type)
                .isInstance(base);
    }

    /**
     * Extracts the value out of the provided tag.
     *
     * @param type the type of the value to extract
     * @param tag  the tag to extract the value from
     * @param <T>  the generic type of the value stored inside the tag
     * @return the extracted value
     * @throws IllegalArgumentException if the passed base is not an instanced of the defined base type and therefore is not applicable to the extractor function
     * @throws IllegalArgumentException if the found object is not of type passed
     * @throws IllegalArgumentException if no suitable tag type adapter for this type was found
     */
    public <T> T extract(Class<T> type, Tag<T> tag) throws ClassCastException, IllegalArgumentException {
        var adapter = obtainAdapter(type);
        Preconditions.checkArgument(adapter.isInstance(tag), "The found tag instance (%s) cannot store %s", tag.getClass().getSimpleName(), type.getSimpleName());

        Object foundValue = adapter.extract(tag);
        Preconditions.checkArgument(type.isInstance(foundValue), "The found object is of the type %s. Expected type %s", foundValue.getClass().getSimpleName(), type.getSimpleName());
        return type.cast(foundValue);
    }

    private record TagAdapter<T, Z extends Tag<?>>(Class<T> primitiveType, Class<Z> nbtBaseType,
                                                   BiFunction<String, T, Z> builder, Function<Z, T> extractor) {
        /**
         * This method will extract the value stored in the tag, according to
         * the expected primitive type.
         *
         * @param base the base to extract from
         * @return the value stored inside of the tag
         * @throws ClassCastException if the passed base is not an instanced of
         *                            the defined base type and therefore is not applicable to the
         *                            extractor function
         */
        T extract(Tag<?> base) {
            Preconditions.checkArgument(this.nbtBaseType.isInstance(base), "The provided NBTBase was of the type %s. Expected type %s", base.getClass().getSimpleName(), this.nbtBaseType.getSimpleName());
            return this.extractor.apply(this.nbtBaseType.cast(base));
        }

        /**
         * Builds a tag instance wrapping around the provided value object.
         *
         * @param value the value to store inside the created tag
         * @return the new tag instance
         * @throws ClassCastException if the passed value object is not of the
         *                            defined primitive type and therefore is not applicable to the builder
         *                            function
         */
        Z build(String key, Object value) {
            Preconditions.checkArgument(this.primitiveType.isInstance(value), "The provided value was of the type %s. Expected type %s", value.getClass().getSimpleName(), this.primitiveType.getSimpleName());
            return this.builder.apply(key, this.primitiveType.cast(value));
        }

        /**
         * Returns if the tag instance matches the adapters one.
         *
         * @param base the base to check
         * @return if the tag was an instance of the set type
         */
        boolean isInstance(Tag<?> base) {
            return this.nbtBaseType.isInstance(base);
        }
    }
}
