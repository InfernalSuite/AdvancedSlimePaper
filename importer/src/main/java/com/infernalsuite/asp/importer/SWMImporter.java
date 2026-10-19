package com.infernalsuite.asp.importer;

import com.infernalsuite.asp.api.world.ExtraRegionFolder;
import com.infernalsuite.asp.serialization.anvil.AnvilImportData;
import com.infernalsuite.asp.serialization.anvil.AnvilWorldReader;
import com.infernalsuite.asp.serialization.slime.SlimeSerializer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class SWMImporter {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java -jar slimeworldmanager-importer.jar <path-to-world-folder> [--accept] [--print-error] [--extra-region <folder>[=<chunk-data-key>]]");
            return;
        }

        File worldDir = new File(args[0]);
        File outputFile = getDestinationFile(worldDir);

        List<String> argList = Arrays.asList(args);
        boolean hasAccepted = argList.contains("--accept");
        boolean printErrors = argList.contains("--print-error");

        List<ExtraRegionFolder> extraRegionFolders = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            if (!args[i].equals("--extra-region")) continue;

            if (++i == args.length) {
                System.err.println("--extra-region requires a <folder>[=<chunk-data-key>] value.");
                return;
            }

            extraRegionFolders.add(ExtraRegionFolder.parse(args[i]));
        }

        if(!hasAccepted) {
            System.out.println("**** WARNING ****");
            System.out.println("The Slime Format is meant to be used on tiny maps, not big survival worlds. It is recommended " +
                    "to trim your world by removing unused chunks using the MCASelector tool to ensure you don't save more chunks than you want to.");
            System.out.println();
            System.out.println("NOTE: This utility will automatically ignore every chunk that doesn't contain any blocks.");
            System.out.print("Do you want to continue? [Y/N]: ");

            Scanner scanner = new Scanner(System.in);
            String response = scanner.next();

            if(!response.equalsIgnoreCase("Y")) {
                System.out.println("Your wish is my command.");
                return;
            }
        }

        importWorld(worldDir, outputFile, printErrors, extraRegionFolders);
    }

    public static void importWorld(File worldDir, File outputFile, boolean shouldPrintDebug) {
        importWorld(worldDir, outputFile, shouldPrintDebug, List.of());
    }

    public static void importWorld(File worldDir, File outputFile, boolean shouldPrintDebug, List<ExtraRegionFolder> extraRegionFolders) {
        try {
            outputFile.createNewFile();
            AnvilImportData importData = new AnvilImportData(worldDir.toPath(), outputFile.getName(), null, extraRegionFolders);
            Files.write(outputFile.toPath(), SlimeSerializer.serialize(AnvilWorldReader.INSTANCE_NO_CONVERSION.readFromData(importData)));
        } catch (IndexOutOfBoundsException ex) {
            System.err.println("Oops, it looks like the world provided is too big to be imported. " +
                    "Please trim it by using the MCEdit tool and try again.");
        } catch (IOException ex) {
            System.err.println("Failed to save the world file.");
            ex.printStackTrace();
        } catch (RuntimeException ex) {
            if(shouldPrintDebug) {
                ex.printStackTrace();
            } else {
                System.err.println(ex.getMessage());
            }
        }
    }

    /**
     * Returns a destination file at which the slime file will
     * be placed when run as an executable.
     *
     * This method may be used by your plugin to output slime
     * files identical to the executable.
     *
     * @param worldFolder The world directory to import
     * @return The output file destination
     */
    public static File getDestinationFile(File worldFolder) {
        return new File(worldFolder.getParentFile(), worldFolder.getName() + ".slime");
    }
}
