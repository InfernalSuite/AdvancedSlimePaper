package com.grinderwolf.swm.plugin.loaders.file;

import com.grinderwolf.swm.plugin.log.Logging;
import com.infernalsuite.aswm.api.exceptions.UnknownWorldException;
import com.infernalsuite.aswm.api.exceptions.WorldLockedException;
import com.infernalsuite.aswm.api.loaders.SlimeLoader;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.NotDirectoryException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FileLoader implements SlimeLoader {

    private static final FilenameFilter WORLD_FILE_FILTER = (dir, name) -> name.endsWith(".slime");

    private final Map<String, RandomAccessFile> worldFiles = Collections.synchronizedMap(new HashMap<>());
    private final File worldDir;

    public FileLoader(File worldDir) {
        this.worldDir = worldDir;

        if (worldDir.exists() && !worldDir.isDirectory()) {
            Logging.warning("A file named '" + worldDir.getName() + "' has been deleted, as this is the name used for the worlds directory.");
            worldDir.delete();
        }

        worldDir.mkdirs();
    }

    @Override
    public byte[] loadWorld(String worldName) throws UnknownWorldException, IOException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

//        RandomAccessFile file = worldFiles.computeIfAbsent(worldName, (world) -> {
//
//            try {
//                return new RandomAccessFile(new File(worldDir, worldName + ".slime"), "rw");
//            } catch (FileNotFoundException ex) {
//                return null; // This is never going to happen as we've just checked if the world exists
//            }
//
//        });

        RandomAccessFile file = new RandomAccessFile(new File(worldDir, worldName + ".slime"), "rw");


        if (file != null && file.length() > Integer.MAX_VALUE) {
            throw new IndexOutOfBoundsException("World is too big!");
        }

        byte[] serializedWorld = new byte[0];
        if (file != null) {
            serializedWorld = new byte[(int) file.length()];
            file.seek(0); // Make sure we're at the start of the file
            file.readFully(serializedWorld);
        }

        return serializedWorld;
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
        RandomAccessFile worldFile = worldFiles.get(worldName);
        boolean tempFile = worldFile == null;

        if (tempFile) {
            worldFile = new RandomAccessFile(new File(worldDir, worldName + ".slime"), "rw");
        }

        worldFile.seek(0); // Make sure we're at the start of the file
        worldFile.setLength(0); // Delete old data
        worldFile.write(serializedWorld);


        worldFile.close();
    }

    @Override
    public void unlockWorld(String worldName) throws UnknownWorldException, IOException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        }

        RandomAccessFile file = worldFiles.remove(worldName);

        if (file != null) {
            FileChannel channel = file.getChannel();
            if (channel.isOpen()) {
                file.close();
            }
        }
    }

    @Override
    public boolean isWorldLocked(String worldName) throws IOException {
        RandomAccessFile file = worldFiles.get(worldName);

        if (file == null) {
            file = new RandomAccessFile(new File(worldDir, worldName + ".slime"), "rw");
        }

        if (file.getChannel().isOpen()) {
            file.close();
        } else {
            return true;
        }
        return false;
    }

    @Override
    public void deleteWorld(String worldName) throws UnknownWorldException {
        if (!worldExists(worldName)) {
            throw new UnknownWorldException(worldName);
        } else {
            try (RandomAccessFile randomAccessFile = worldFiles.get(worldName)) {
                System.out.println("Deleting world.. " + worldName + ".");
                FileUtils.forceDelete(new File(worldDir, worldName + ".slime"));
                if (randomAccessFile != null) {
                    System.out.print("Attempting to delete worldData " + worldName + ".");

                    randomAccessFile.seek(0); // Make sure we're at the start of the file
                    randomAccessFile.setLength(0); // Delete old data
                    randomAccessFile.write(null);
                    randomAccessFile.close();

                    worldFiles.remove(worldName);
                }
                System.out.println("World.. " + worldName + " deleted.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void acquireLock(String worldName) throws UnknownWorldException, WorldLockedException, IOException {
        RandomAccessFile worldFile = worldFiles.get(worldName);
        boolean tempFile = worldFile == null;

        if (tempFile) {
            worldFile = new RandomAccessFile(new File(worldDir, worldName + ".slime"), "rw");
        }

        FileChannel channel = worldFile.getChannel();

        try {
            channel.tryLock();
        } catch (OverlappingFileLockException ignored) {
            throw new WorldLockedException(worldName);
        }
    }
}
