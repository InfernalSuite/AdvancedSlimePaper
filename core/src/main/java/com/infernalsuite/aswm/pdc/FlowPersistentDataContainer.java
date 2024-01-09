package com.infernalsuite.aswm.pdc;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.Tag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import com.google.common.base.Preconditions;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class FlowPersistentDataContainer implements PersistentDataContainer, PersistentDataAdapterContext {

    private final CompoundTag root;
    private final FlowDataTypeRegistry registry;

    public FlowPersistentDataContainer(CompoundTag root, FlowDataTypeRegistry typeRegistry) {
        this.root = root;
        this.registry = typeRegistry;
    }

    public FlowPersistentDataContainer(CompoundTag root) {
        this.root = root;
        this.registry = FlowDataTypeRegistry.DEFAULT;
    }

    protected CompoundTag getRoot() {
        return root;
    }

    @Override
    public <T, Z> void set(@NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type, @NotNull Z value) {
        var name = key.toString();
        root.getValue().put(name, registry.wrap(name, type.getPrimitiveType(), type.toPrimitive(value, getAdapterContext())));
    }

    @Override
    public <T, Z> boolean has(@NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type) {
        var value = root.getValue().get(key.toString());

        if (value == null) {
            return false;
        }

        return registry.isInstanceOf(type.getPrimitiveType(), value);
    }

    @Override
    public <T, Z> @Nullable Z get(@NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type) {
        var value = root.getValue().get(key.toString());

        if (value == null) {
            return null;
        }

        return type.fromPrimitive(registry.extract(type.getPrimitiveType(), (Tag<T>) value), getAdapterContext());
    }

    @Override
    public <T, Z> @NotNull Z getOrDefault(@NotNull NamespacedKey key, @NotNull PersistentDataType<T, Z> type, @NotNull Z defaultValue) {
        var value = get(key, type);
        return value == null ? defaultValue : value;
    }

    @Override
    public @NotNull Set<NamespacedKey> getKeys() {
        var keys = new HashSet<NamespacedKey>();

        for (String key : root.getValue().keySet()) {
            String[] keyData = key.split(":", 2);
            if (keyData.length == 2) {
                keys.add(new NamespacedKey(keyData[0], keyData[1]));
            }
        }

        return keys;
    }

    @Override
    public void remove(@NotNull NamespacedKey key) {
        root.getValue().remove(key.toString());
    }

    @Override
    public boolean isEmpty() {
        return root.getValue().isEmpty();
    }

    @Override
    public void copyTo(@NotNull PersistentDataContainer other, boolean replace) {
        Preconditions.checkNotNull(other, "The target container cannot be null");

        if (other instanceof FlowPersistentDataContainer otherFlow) {
            if (replace) {
                otherFlow.root.setValue(this.root.getValue());
            } else {
                otherFlow.root.getValue().forEach((k, v) -> otherFlow.root.getValue().putIfAbsent(k, v));
            }
        } else {
            throw new IllegalStateException("Cannot copy to a container that isn't a FlowPersistentDataContainer");
        }
    }

    @Override
    public @NotNull PersistentDataAdapterContext getAdapterContext() {
        return this;
    }

    @Override
    public boolean has(@NotNull NamespacedKey key) {
        return root.getValue().containsKey(key.toString());
    }

    @Override
    public byte @NotNull [] serializeToBytes() throws IOException {
        if (root == null || root.getValue().isEmpty()) {
            return new byte[0];
        }
        ByteArrayOutputStream outByteStream = new ByteArrayOutputStream();
        NBTOutputStream outStream = new NBTOutputStream(outByteStream, NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
        outStream.writeTag(root);

        return outByteStream.toByteArray();
    }

    @Override
    public void readFromBytes(byte @NotNull [] bytes, boolean clear) throws IOException {
        if (bytes.length == 0) {
            return;
        }

        NBTInputStream stream = new NBTInputStream(new ByteArrayInputStream(bytes), NBTInputStream.NO_COMPRESSION, ByteOrder.BIG_ENDIAN);
        var compound = (com.flowpowered.nbt.CompoundTag) stream.readTag();

        if (clear) {
            root.getValue().clear();
        }

        root.getValue().putAll(compound.getValue());
    }

    @Override
    public @NotNull PersistentDataContainer newPersistentDataContainer() {
        return new FlowPersistentDataContainer(new CompoundTag("root", new CompoundMap()), registry);
    }

    @Override
    public int hashCode() {
        int hashCode = 3;
        hashCode += root.hashCode(); // We will simply add the tag hashcode
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof FlowPersistentDataContainer flow)) {
            return false;
        }

        return Objects.equals(root, flow.root);
    }
}
