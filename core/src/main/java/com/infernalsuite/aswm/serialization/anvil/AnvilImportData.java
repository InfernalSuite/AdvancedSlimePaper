package com.infernalsuite.aswm.serialization.anvil;

import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import org.jetbrains.annotations.Nullable;

import java.io.File;

public record AnvilImportData(File worldDir, String newName, @Nullable SlimeLoader loader) {
}
