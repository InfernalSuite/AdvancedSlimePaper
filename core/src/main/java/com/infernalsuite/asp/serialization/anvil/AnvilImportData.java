package com.infernalsuite.asp.serialization.anvil;

import com.infernalsuite.asp.api.loaders.SlimeLoader;
import com.infernalsuite.asp.api.world.ExtraRegionFolder;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public record AnvilImportData(Path worldDir, String newName, @Nullable SlimeLoader loader, List<ExtraRegionFolder> extraRegionFolders) {

    public AnvilImportData(Path worldDir, String newName, @Nullable SlimeLoader loader) {
        this(worldDir, newName, loader, List.of());
    }

    public static AnvilImportData legacy(File worldDir, String newName, @Nullable SlimeLoader loader) {
        return new AnvilImportData(worldDir.toPath(), newName, loader);
    }

}
