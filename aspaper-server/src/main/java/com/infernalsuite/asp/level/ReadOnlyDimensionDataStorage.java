package com.infernalsuite.asp.level;

import com.mojang.datafixers.DataFixer;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/*
 * This dimension data storage does not serialize and/or load from disk.
 */
public class ReadOnlyDimensionDataStorage extends DimensionDataStorage {

    public ReadOnlyDimensionDataStorage(SavedData.Context ctx, Path dataFolder, DataFixer fixerUpper, HolderLookup.Provider registries) {
        super(ctx, dataFolder, fixerUpper, registries);
    }

    @Override
    public @Nullable <T extends SavedData> T get(SavedDataType<T> type) {
        Optional<SavedData> optional = this.cache.get(type);
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
