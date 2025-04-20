package com.infernalsuite.asp.api.world.properties.type;

import com.google.common.base.Preconditions;
import com.infernalsuite.asp.api.world.properties.SlimeProperty;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagType;
import net.kyori.adventure.nbt.ListBinaryTag;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public class SlimePropertyList<T, Z extends BinaryTag> extends SlimeProperty<List<T>, ListBinaryTag> {

    public static <T, Z extends BinaryTag> SlimePropertyList<T, Z> create(
            final @NotNull String key,
            final @NotNull List<T> defaultValue,
            final @NotNull BinaryTagType<Z> listTagElementType,
            final @NotNull Function<T, Z> elementTagConverter,
            final @NotNull Function<Z, T> elementTagExtractor
    ) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(defaultValue, "Default value cannot be null");
        Preconditions.checkNotNull(listTagElementType, "List tag element type cannot be null");
        Preconditions.checkNotNull(elementTagConverter, "Element tag converter cannot be null");
        Preconditions.checkNotNull(elementTagExtractor, "Element tag extractor cannot be null");
        return new SlimePropertyList<>(key, defaultValue, listTagElementType, elementTagConverter, elementTagExtractor);
    }

    public static <T, Z extends BinaryTag> SlimePropertyList<T, Z> create(
            final @NotNull String key,
            final @NotNull List<T> defaultValue,
            final @NotNull Function<List<T>, Boolean> validator,
            final @NotNull BinaryTagType<Z> listTagElementType,
            final @NotNull Function<T, Z> elementTagConverter,
            final @NotNull Function<Z, T> elementTagExtractor
    ) {
        Preconditions.checkNotNull(key, "Key cannot be null");
        Preconditions.checkNotNull(defaultValue, "Default value cannot be null");
        Preconditions.checkNotNull(validator, "Use SlimePropertyList#create(String, List<T>) instead");
        Preconditions.checkNotNull(listTagElementType, "List tag element type cannot be null");
        Preconditions.checkNotNull(elementTagConverter, "Element tag converter cannot be null");
        Preconditions.checkNotNull(elementTagExtractor, "Element tag extractor cannot be null");
        return new SlimePropertyList<>(key, defaultValue, validator, listTagElementType, elementTagConverter, elementTagExtractor);
    }

    private final BinaryTagType<Z> listTagElementType;
    private final Function<T, Z> elementTagConverter;
    private final Function<Z, T> elementTagExtractor;

    private SlimePropertyList(String key, List<T> defaultValue, BinaryTagType<Z> listTagElementType, Function<T, Z> elementTagConverter, Function<Z, T> elementTagExtractor) {
        super(key, defaultValue);
        this.listTagElementType = listTagElementType;
        this.elementTagConverter = elementTagConverter;
        this.elementTagExtractor = elementTagExtractor;
    }

    private SlimePropertyList(String key, List<T> defaultValue, Function<List<T>, Boolean> validator, BinaryTagType<Z> listTagElementType, Function<T, Z> elementTagConverter, Function<Z, T> elementTagExtractor) {
        super(key, defaultValue, validator);
        this.listTagElementType = listTagElementType;
        this.elementTagConverter = elementTagConverter;
        this.elementTagExtractor = elementTagExtractor;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected ListBinaryTag createTag(final List<T> value) {
        return ListBinaryTag.listBinaryTag(this.listTagElementType, (List<BinaryTag>) value.stream()
                .map(this.elementTagConverter)
                .toList()
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    protected List<T> readValue(final ListBinaryTag tag) {
        return tag.stream()
                .map(rawTag -> (Z) rawTag)
                .map(this.elementTagExtractor)
                .toList();
    }

    @Override
    protected ListBinaryTag cast(final BinaryTag rawTag) {
        return (ListBinaryTag) rawTag;
    }

}
