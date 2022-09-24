## Loading Worlds

First, retrieve the SlimeWorldManager plugin API:
```java
SlimePlugin plugin = (SlimePlugin) Bukkit.getPluginManager().getPlugin("SlimeWorldManager");
```
Now, you need a loader. A SlimeLoader is a class that reads and stores worlds from a data source. In this case, we'll be using the MySQL loader:
```java
SlimeLoader sqlLoader = plugin.getLoader("mysql");
```

Before actually loading the world, you need a SlimePropertyMap Object. Check the [property API documentation](properties.md) for further details.

That's it, you've got everything you need! Now, let's load the world from the data source and generate it:
```java
try {
    // note that this method should be called asynchronously
    SlimeWorld world = plugin.loadWorld(sqlLoader, "my-world", props);

    // note that this method must be called synchronously
    plugin.generateWorld(world);
} catch (UnknownWorldException | IOException | CorruptedWorldException | NewerFormatException | WorldInUseException | UnsupportedWorldException exception) {
    // exception handling
}
```
