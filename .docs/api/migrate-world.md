## Migrating Worlds

To migrate a world you need three things: a world name, the data source where the world is currently stored in and another data source to store the world. Here's an example of a world migration:
```java
SlimePlugin plugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");

String worldName = "my_world";
SlimeLoader currentLoader = plugin.getLoader("mysql");
SlimeLoader newLoader = plugin.getLoader("mongodb");

try {
    // note that this method should be called asynchronously
    plugin.migrateWorld(worldName, currentLoader, newLoader);
} catch (IOException | WorldInUseException | WorldAlreadyExistsException | UnknownWorldException exception) {
    // exception handling
}
```
