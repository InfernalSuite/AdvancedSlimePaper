# World Configuration

To configure a world, open the 'worlds.yml' file inside the ASWM config folder. Here is an example of a worlds.yml file:
```yaml
worlds:
    my_great_world:
        source: mongodb
        loadOnStartup: false
        readOnly: true
        spawn: 940, 2, -370
        allowMonsters: false
        allowAnimals: false
        difficulty: peaceful
        pvp: false
        environment: NORMAL
        worldType: default
```

Then, save it and reload it by using the /swm reload command. You're good to go!

## Config options
#### `source`
Description: the name of the data source where the world is stored.<br>
Available options: `file`, `mysql`, `mongodb`. Any other datasources provided by third-party plugins can also be used.<br>
Defaults to: `file`.

#### `loadOnStartup`
Description: whether or not the world should be loaded when the server starts up.<br>
Available options: `true` and `false`.

#### `readOnly`
Description: if true, changes to the world will never be stored. If false, the world will be locked, so no other server can access it without being on read-only mode.<br>
Available options: `true` and `false`.<br>
Defaults to: `false`.

#### `spawn`
Description: spawn coordinates for the world.<br>
Available options: `<x-coord>, <y-coord>, <z-coord>`.<br>
Defaults to: `0, 255, 0`.

#### `allowMonsters`
Description: whether or not monsters can spawn on this world.<br>
Available options: `true` and `false`.<br>
Defaults to: `true`.

#### `allowAnimals`
Description: whether or not animals can spawn on this world.<br>
Available options: `true` and `false`.<br>
Defaults to: `true`.

#### `difficulty`
Description: the difficulty of the world.<br>
Available options: `peaceful`, `easy`, `normal` and `hard`.<br>
Defaults to: `peaceful`.

#### `pvp`
Description: if true, PvP will be allowed on this world.<br>
Available options: `true` and `false`.<br>
Defaults to: `true`.

#### `environment`
Description: sets the world environment.<br>
Available options: `normal`, `nether`, `the_end`.<br>
Defaults to: `normal`.

#### `worldType`
Description: sets the level type.<br>
Available options: `default`, `flat`, `large_biomes`, `amplified`, `customized`, `debug_all_block_states`, `default_1_1`.<br>
Defaults to: `default`.
