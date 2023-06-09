# Converting Worlds

To be able to load a world with ASWM, you have to convert it to the SRF. There are two ways of doing this:

## Using the in-game command

1. Place your world inside your server's root directory.
2. Make sure the world is unloaded. Loaded worlds cannot be converted.
3. Run the command `/aswm import <your-world-folder> <data-source> [new-world-name]`. If you want the world to have the same name as the world folder, simply ignore the _[new-world-name]_ argument.
4. Done! The world is now inside the data source you've provided.

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
