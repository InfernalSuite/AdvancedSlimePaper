package com.infernalsuite.asp.serialization.anvil;

import com.infernalsuite.asp.api.loaders.SlimeLoader;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public record AnvilImportData(File worldDir, String newName, @Nullable SlimeLoader loader) {
}
