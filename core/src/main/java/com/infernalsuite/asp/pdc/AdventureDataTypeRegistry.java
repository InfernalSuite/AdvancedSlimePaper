package com.infernalsuite.asp.pdc;

import com.google.common.base.Preconditions;
import com.infernalsuite.asp.api.SlimeNMSBridge;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagType;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.ByteArrayBinaryTag;
import net.kyori.adventure.nbt.ByteBinaryTag;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.DoubleBinaryTag;
import net.kyori.adventure.nbt.FloatBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.IntBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.kyori.adventure.nbt.LongArrayBinaryTag;
import net.kyori.adventure.nbt.LongBinaryTag;
import net.kyori.adventure.nbt.ShortBinaryTag;
import net.kyori.adventure.nbt.StringBinaryTag;
import org.bukkit.persistence.ListPersistentDataType;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AdventureDataTypeRegistry {

    public static final AdventureDataTypeRegistry DEFAULT = new AdventureDataTypeRegistry();
    private final ConcurrentMap<Class<?>, TagAdapter<?,?>> adapters = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    private <P, T extends BinaryTag> TagAdapter<P, T> obtainAdapter(final PersistentDataType<P, ?> dataType) {
        return (TagAdapter<P, T>) this.adapters.computeIfAbsent(dataType.getPrimitiveType(), this::createAdapter);
    }

    @SuppressWarnings("unchecked")
    private <P, T extends BinaryTag> TagAdapter<P, T> createAdapter(final Class<P> primitiveType) {
        if (TagAdapter.PRIMITIVE_ADAPTERS.containsKey(primitiveType)) {
            return (TagAdapter<P, T>) TagAdapter.PRIMITIVE_ADAPTERS.get(primitiveType);
        } else if (PersistentDataContainer.class.isAssignableFrom(primitiveType)) {
            return (TagAdapter<P, T>) TagAdapter.of(PersistentDataContainer.class, CompoundBinaryTag.class, BinaryTagTypes.COMPOUND, this::extractPDC, this::buildPDC);
        } else if (primitiveType.isArray() && PersistentDataContainer.class.isAssignableFrom(primitiveType.componentType())) {
            return (TagAdapter<P, T>) TagAdapter.of(PersistentDataContainer[].class, ListBinaryTag.class, BinaryTagTypes.LIST, containers -> {
                ListBinaryTag.Builder<CompoundBinaryTag> builder = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);
                for (PersistentDataContainer container : containers) {
                    builder.add(this.extractPDC(container));
                }
                return builder.build();
            }, tag -> {
                PersistentDataContainer[] containers = new PersistentDataContainer[tag.size()];
                for (int i = 0; i < tag.size(); i++) {
                    containers[i] = this.buildPDC(tag.getCompound(i));
                }
                return containers;
            });
        } else if (List.class.isAssignableFrom(primitiveType)) {
            return (TagAdapter<P, T>) TagAdapter.of(List.class, ListBinaryTag.class, BinaryTagTypes.LIST, this::constructList, this::extractList, this::matchesListTag);
        } else {
            throw new IllegalArgumentException("Could not find a valid TagAdapter implementation for the requested type " + primitiveType.getSimpleName());
        }
    }

    private CompoundBinaryTag extractPDC(PersistentDataContainer pdc) {
        CompoundBinaryTag.Builder builder = CompoundBinaryTag.builder();

        if (pdc instanceof AdventurePersistentDataContainer container) {
            builder.put(container.getRaw());
        } else {
            SlimeNMSBridge.instance().extractCraftPDC(pdc, builder);
        }

        return builder.build();
    }

    private PersistentDataContainer buildPDC(CompoundBinaryTag tag) {
        return new AdventurePersistentDataContainer(tag, this);
    }

    public <P> BinaryTag wrap(final PersistentDataType<P, ?> dataType, final P value) throws IllegalArgumentException {
        return this.obtainAdapter(dataType).build(dataType, value);
    }

    public <P> boolean isInstanceOf(final PersistentDataType<P, ?> dataType, final BinaryTag tag) throws IllegalArgumentException{
        return this.obtainAdapter(dataType).isInstance(dataType, tag);
    }

    public <P, T extends BinaryTag> P extract(final PersistentDataType<P, ?> dataType, final BinaryTag tag) throws ClassCastException, IllegalArgumentException {
        final Class<P> primitiveType = dataType.getPrimitiveType();
        final TagAdapter<P, T> adapter = this.obtainAdapter(dataType);
        Preconditions.checkArgument(adapter.isInstance(dataType, tag), "The found tag instance (%s) cannot store %s", tag.getClass().getSimpleName(), primitiveType.getSimpleName());

        final P foundValue = adapter.extract(dataType, tag);
        Preconditions.checkArgument(primitiveType.isInstance(foundValue), "The found object is type %s, expected type %s", foundValue.getClass().getSimpleName(), primitiveType.getSimpleName());
        return foundValue;
    }

    private <P, L extends List<P>> ListBinaryTag constructList(final PersistentDataType<L, ?> dataType, final List<P> list) {
        Preconditions.checkArgument(dataType instanceof ListPersistentDataType<?,?>, "The passed list cannot be written to the PDC with a %s (expected a list data type", dataType.getClass().getSimpleName());
        @SuppressWarnings("unchecked") final ListPersistentDataType<P, ?> listDataType = (ListPersistentDataType<P, ?>) dataType;

        final ListBinaryTag.Builder<BinaryTag> builder = ListBinaryTag.builder();
        list.forEach(primitive -> builder.add(this.wrap(listDataType.elementType(), primitive)));

        return builder.build();
    }

    private <P> List<P> extractList(final PersistentDataType<P, ?> dataType, final ListBinaryTag listTag) {
        Preconditions.checkArgument(dataType instanceof ListPersistentDataType<?, ?>, "The found list tag cannot be read with a %s (expected a list data type)", dataType.getClass().getSimpleName());
        @SuppressWarnings("unchecked") final ListPersistentDataType<P, ?> listDataType = (ListPersistentDataType<P, ?>) dataType;

        return listTag.stream().map(tag -> this.extract(listDataType.elementType(), tag)).collect(Collectors.toList());
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private boolean matchesListTag(final PersistentDataType<List, ?> dataType, final BinaryTag tag) {
        if (!(dataType instanceof final ListPersistentDataType listDataType)) return false;
        if (!(tag instanceof final ListBinaryTag listTag)) return false;

        final TagAdapter<?, ?> adapter = this.obtainAdapter(listDataType.elementType());

        return adapter.nbtType().id() == listTag.elementType().id();
    }

    private record TagAdapter<P, T extends BinaryTag>(
            Class<P> primitiveType,
            Class<T> tagType,
            BinaryTagType<T> nbtType,
            BiFunction<PersistentDataType<P, ?>, P, T> builder,
            BiFunction<PersistentDataType<P, ?>, T, P> extractor,
            BiPredicate<PersistentDataType<P, ?>, BinaryTag> matcher
    ) {
        private static final Map<Class<?>, TagAdapter<?, ?>> PRIMITIVE_ADAPTERS = initPrimitiveAdapters();

        private static <P, T extends BinaryTag> TagAdapter<P, T> of(Class<P> primitiveType, Class<T> tagType, BinaryTagType<T> nbtType, Function<P, T> builder, Function<T, P> extractor) {
            return of(primitiveType, tagType, nbtType, (type, p) -> builder.apply(p), (type, t) -> extractor.apply(t), (type, tag) -> tagType.isInstance(tag));
        }

        private static <P, T extends BinaryTag> TagAdapter<P, T> of(Class<P> primitiveType, Class<T> tagType, BinaryTagType<T> nbtType, BiFunction<PersistentDataType<P, ?>, P, T> builder, BiFunction<PersistentDataType<P, ?>, T, P> extractor, BiPredicate<PersistentDataType<P, ?>, BinaryTag> matcher) {
            return new TagAdapter<>(primitiveType, tagType, nbtType, builder, extractor, matcher);
        }

        private P extract(final PersistentDataType<P, ?> dataType, final BinaryTag tag) {
            Preconditions.checkArgument(this.tagType.isInstance(tag), "The provided tag was type %s, expected %s", tag.getClass().getSimpleName(), this.tagType.getSimpleName());
            return this.extractor.apply(dataType, this.tagType.cast(tag));
        }

        T build(final PersistentDataType<P, ?> dataType, final P value) {
            Preconditions.checkArgument(this.primitiveType.isInstance(value), "The provided value was type %s, expected %s", value.getClass().getSimpleName(), this.primitiveType.getSimpleName());
            return this.builder.apply(dataType, value);
        }

        boolean isInstance(final PersistentDataType<P, ?> dataType, final BinaryTag tag) {
            return this.matcher.test(dataType, tag);
        }

        private static Map<Class<?>, TagAdapter<?, ?>> initPrimitiveAdapters() {
            final Map<Class<?>, TagAdapter<?, ?>> adapters = new IdentityHashMap<>();
            adapters.put(Byte.class, TagAdapter.of(Byte.class, ByteBinaryTag.class, BinaryTagTypes.BYTE, ByteBinaryTag::byteBinaryTag, ByteBinaryTag::value));
            adapters.put(Short.class, TagAdapter.of(Short.class, ShortBinaryTag.class, BinaryTagTypes.SHORT, ShortBinaryTag::shortBinaryTag, ShortBinaryTag::value));
            adapters.put(Integer.class, TagAdapter.of(Integer.class, IntBinaryTag.class, BinaryTagTypes.INT, IntBinaryTag::intBinaryTag, IntBinaryTag::value));
            adapters.put(Long.class, TagAdapter.of(Long.class, LongBinaryTag.class, BinaryTagTypes.LONG, LongBinaryTag::longBinaryTag, LongBinaryTag::value));
            adapters.put(Float.class, TagAdapter.of(Float.class, FloatBinaryTag.class, BinaryTagTypes.FLOAT, FloatBinaryTag::floatBinaryTag, FloatBinaryTag::value));
            adapters.put(Double.class, TagAdapter.of(Double.class, DoubleBinaryTag.class, BinaryTagTypes.DOUBLE, DoubleBinaryTag::doubleBinaryTag, DoubleBinaryTag::value));
            adapters.put(String.class, TagAdapter.of(String.class, StringBinaryTag.class, BinaryTagTypes.STRING, StringBinaryTag::stringBinaryTag, StringBinaryTag::value));
            adapters.put(byte[].class, TagAdapter.of(byte[].class, ByteArrayBinaryTag.class, BinaryTagTypes.BYTE_ARRAY, ByteArrayBinaryTag::byteArrayBinaryTag, ByteArrayBinaryTag::value));
            adapters.put(int[].class, TagAdapter.of(int[].class, IntArrayBinaryTag.class, BinaryTagTypes.INT_ARRAY, IntArrayBinaryTag::intArrayBinaryTag, IntArrayBinaryTag::value));
            adapters.put(long[].class, TagAdapter.of(long[].class, LongArrayBinaryTag.class, BinaryTagTypes.LONG_ARRAY, LongArrayBinaryTag::longArrayBinaryTag, LongArrayBinaryTag::value));
            return adapters;
        }

    }

}
