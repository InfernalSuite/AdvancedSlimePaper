package com.infernalsuite.asp.level;

import com.mojang.datafixers.DataFixer;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class ReadOnlyDimensionDataStorage extends DimensionDataStorage {

    public ReadOnlyDimensionDataStorage(Path dataFolder, DataFixer fixerUpper, HolderLookup.Provider registries) {
        super(dataFolder, fixerUpper, registries);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable <T extends SavedData> T get(SavedData.@NotNull Factory<T> factory, @NotNull String name) {
        Optional<SavedData> optional = this.cache.get(name);
        if(optional == null) {
            return null;
        }
        return (T) optional.orElse(null);
    }

    @Override
    public @NotNull CompletableFuture<?> scheduleSave() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void saveAndJoin() {}

    @Override
    public void close() {}

}
