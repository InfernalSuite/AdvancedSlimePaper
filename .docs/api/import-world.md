## Importing Worlds

You need three things to import a world: a world folder, a world name and a data source. Here's an example of how to import a world:
```java
SlimePlugin plugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");

File worldDir = new File("my_world_foler");
String worldName = "my_world";
SlimeLoader loader = plugin.getLoader("mysql");

try {
    // note that this method should be called asynchronously
    plugin.importWorld(worldDir, worldName, loader);
} catch (WorldAlreadyExistsException | InvalidWorldException | WorldLoadedException | WorldTooBigException | IOException exception) {
    // exception handling
}
```
