package com.infernalsuite.aswm.loaders.file;

import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.NotDirectoryException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class FileLoader implements SlimeLoader {

    private static final FilenameFilter WORLD_FILE_FILTER = (dir, name) -> name.endsWith(".slime");
    private static final Logger LOGGER = LoggerFactory.getLogger(FileLoader.class);

    private final File worldDir;

    public FileLoader(File worldDir) throws IllegalStateException {
        this.worldDir = worldDir;

        if (worldDir.exists() && !worldDir.isDirectory()) {
            LOGGER.warn("A file named '{}' has been deleted, as this is the name used for the worlds directory.", worldDir.getName());
            if (!worldDir.delete()) throw new IllegalStateException("Failed to delete the file named '" + worldDir.getName() + "'.");
        }

        if (!worldDir.exists() && !worldDir.mkdirs()) throw new IllegalStateException("Failed to create the worlds directory.");
    }

    @Override
    public byte[] readWorld(String worldName) throws UnknownWorldException, IOException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        try (FileInputStream fis = new FileInputStream(new File(worldDir, worldName + ".slime"))){
            return fis.readAllBytes();
        }
    }

    @Override
    public boolean worldExists(String worldName) {
        return new File(worldDir, worldName + ".slime").exists();
    }

    @Override
    public List<String> listWorlds() throws NotDirectoryException {
        String[] worlds = worldDir.list(WORLD_FILE_FILTER);

        if (worlds == null) {
            throw new NotDirectoryException(worldDir.getPath());
        }

        return Arrays.stream(worlds).map((c) -> c.substring(0, c.length() - 6)).collect(Collectors.toList());
    }

    @Override
    public void saveWorld(String worldName, byte[] serializedWorld) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(new File(worldDir, worldName + ".slime"))) {
            fos.write(serializedWorld);
        }
    }

    @Override
    public void deleteWorld(String worldName) throws UnknownWorldException, IOException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        } else {
            if (!new File(worldDir, worldName + ".slime").delete()) {
                throw new IOException("Failed to delete the world file. File#delete() returned false.");
            }
        }
    }
}
