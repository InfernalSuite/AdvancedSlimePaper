# Converting Worlds

To be able to load a world with ASWM, you have to convert it to the SRF. There are two ways of doing this:

## Using the in-game command

1. Place your world inside your server's root directory.
2. Make sure the world is unloaded. Loaded worlds cannot be converted.
3. Run the command `/swm import <your-world-folder> <data-source> [new-world-name]`. If you want the world to have the same name as the world folder, simply ignore the _[new-world-name]_ argument.
4. Done! The world is now inside the data source you've provided.

## Using the importer tool (Advanced)

If you prefer to import worlds externally, or want to import worlds as part of an automated process,
the importer tool may provide a better workflow for you. The importer tool executes as a standalone java
executable and converts a world directory into a slime world file.

Here are the steps to be followed:
1. Place your world alongside the importer tool.
2. Open a command prompt (cmd).
3. Type this command:

```
java -jar slimeworldmanager-importer.jar <your-world-folder>
```

It'll automatically scan your world and generate a .slime file inside the same directory you placed it. That's your world!

### Commandline arguments

The importer tool provides some command line arguments to configure the behavior of the importer.

- `--silent` - Disables the printing of progress messages
- `--accept` - Automatically accept the warning notice
- `--print-error` - Print the full stack trace in the case of an error, intended for debugging

### Usage as API

The importer tool may be used as a dependency in your projects to import worlds programmatically.

The basic usage of the API is as follows:
```java
File theOutputFile = SWMImporter.getDestinationFile(theWorldDir);

try {
    SWMImporter.importWorld(theWorldDir, theOutputFile, true);
} catch (IOException | InvalidWorldException exception) {
    // exception handling
}
```
