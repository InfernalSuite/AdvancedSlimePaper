package com.infernalsuite.asp.pdc;

import com.google.common.base.Preconditions;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagIO;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AdventurePersistentDataContainer implements PersistentDataContainer, PersistentDataAdapterContext {

    private static final Pattern NS_KEY_PATTERN = Pattern.compile(":");

    private final ConcurrentMap<String, BinaryTag> tags = new ConcurrentHashMap<>();
    private final AdventureDataTypeRegistry registry;

    public AdventurePersistentDataContainer() {
        this(AdventureDataTypeRegistry.DEFAULT);
    }

    public AdventurePersistentDataContainer(final AdventureDataTypeRegistry registry) {
        this.registry = registry;
    }

    public AdventurePersistentDataContainer(final Map<String, BinaryTag> tags, final AdventureDataTypeRegistry registry) {
        this(registry);
        this.tags.putAll(tags);
    }

    public AdventurePersistentDataContainer(final CompoundBinaryTag root, final AdventureDataTypeRegistry registry) {
        this(registry);
        root.forEach(entry -> this.tags.put(entry.getKey(), entry.getValue()));
    }

    public AdventurePersistentDataContainer(final CompoundBinaryTag root) {
        this(root, AdventureDataTypeRegistry.DEFAULT);
    }

    public AdventurePersistentDataContainer(final Map<String, BinaryTag> tags) {
        this(tags, AdventureDataTypeRegistry.DEFAULT);
    }

    Map<String, BinaryTag> getRaw() {
        return this.tags;
    }

    public Map<String, BinaryTag> getTags() {
        return Collections.unmodifiableMap(this.tags);
    }

    public CompoundBinaryTag toCompound() {
        return CompoundBinaryTag.builder().put(this.tags).build();
    }

    @Override
    public <P, C> void set(@NotNull NamespacedKey key, @NotNull PersistentDataType<P, C> type, @NotNull C value) {
        Preconditions.checkNotNull(key, "The key cannot be null");
        Preconditions.checkNotNull(type, "The provided type cannot be null");
        Preconditions.checkNotNull(value, "The provided value cannot be null");
        this.tags.put(key.toString(), this.registry.wrap(type, type.toPrimitive(value, getAdapterContext())));
    }

    @Override
    public <P, C> boolean has(@NotNull NamespacedKey key, @NotNull PersistentDataType<P, C> type) {
        Preconditions.checkNotNull(key, "The key cannot be null");
        Preconditions.checkNotNull(type, "The provided type cannot be null");

        BinaryTag tag = this.tags.get(key.toString());

        return tag != null && this.registry.isInstanceOf(type, tag);
    }

    @Override
    public boolean has(@NotNull NamespacedKey key) {
        Preconditions.checkNotNull(key, "The key cannot be null");
        return this.tags.containsKey(key.toString());
    }

    @Override
    public <P, C> @Nullable C get(@NotNull NamespacedKey key, @NotNull PersistentDataType<P, C> type) {
        Preconditions.checkNotNull(key, "The key cannot be null");
        Preconditions.checkNotNull(type, "The provided type cannot be null");

        final BinaryTag tag = this.tags.get(key.toString());

        return tag == null ? null : type.fromPrimitive(this.registry.extract(type, tag), getAdapterContext());
    }

    @Override
    public <P, C> @NotNull C getOrDefault(@NotNull NamespacedKey key, @NotNull PersistentDataType<P, C> type, @NotNull C defaultValue) {
        final C value = this.get(key, type);
        return value == null ? defaultValue : value;
    }

    @Override
    public @NotNull Set<NamespacedKey> getKeys() {
        return this.tags.keySet().stream()
                .map(key -> NS_KEY_PATTERN.split(key, 2))
                .filter(keyData -> keyData.length == 2)
                .map(keyData -> new NamespacedKey(keyData[0], keyData[1]))
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void remove(@NotNull NamespacedKey key) {
        Preconditions.checkNotNull(key, "The key cannot be null");
        this.tags.remove(key.toString());
    }

    @Override
    public boolean isEmpty() {
        return this.tags.isEmpty();
    }

    @Override
    public void copyTo(@NotNull PersistentDataContainer other, boolean replace) {
        Preconditions.checkNotNull(other, "The provided container cannot be null");

        if (other instanceof AdventurePersistentDataContainer container) {
            if (replace) {
                container.tags.putAll(this.tags);
            } else {
                this.tags.forEach(container.tags::putIfAbsent);
            }
        } else {
            throw new IllegalArgumentException("Cannot copy to a container that isn't an AdventurePersistentDataContainer (got " + other.getClass().getName() + ")");
        }
    }

    @Override
    public @NotNull PersistentDataAdapterContext getAdapterContext() {
        return this;
    }

    @Override
    public byte @NotNull [] serializeToBytes() throws IOException {
        if (this.tags.isEmpty()) return new byte[0];

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        BinaryTagIO.writer().write(CompoundBinaryTag.builder().put(this.tags).build(), outputStream);

        return outputStream.toByteArray();
    }

    @Override
    public void readFromBytes(byte @NotNull [] bytes, boolean clear) throws IOException {
        if (clear) this.tags.clear();
        if (bytes.length == 0) return;

        BinaryTagIO.unlimitedReader().read(new ByteArrayInputStream(bytes)).forEach(entry -> this.tags.put(entry.getKey(), entry.getValue()));
    }

    @Override
    public @NotNull PersistentDataContainer newPersistentDataContainer() {
        return new AdventurePersistentDataContainer(this.registry);
    }

    @Override
    public int hashCode() {
        int hashCode = 3;
        hashCode =+ this.tags.hashCode();
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;

        AdventurePersistentDataContainer other = (AdventurePersistentDataContainer) obj;
        return this.tags.equals(other.tags);
    }
}
