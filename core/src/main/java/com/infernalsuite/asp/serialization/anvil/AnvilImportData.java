package com.infernalsuite.asp.serialization.anvil;

import com.infernalsuite.asp.api.loaders.SlimeLoader;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;

public record AnvilImportData(Path worldDir, String newName, @Nullable SlimeLoader loader) {

    public static AnvilImportData legacy(File worldDir, String newName, @Nullable SlimeLoader loader) {
        return new AnvilImportData(worldDir.toPath(), newName, loader);
    }

}
