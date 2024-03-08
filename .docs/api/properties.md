# Properties API

Property "types" are handled by [SlimeProperty][1] instances. Whilst creating [SlimeProperty][1] objects is not allowed, there is a [list of all available properties][2]. Properties and their values are stored in [SlimePropertyMaps][3].


**Example Usage:**
```java
// create a new and empty property map
SlimePropertyMap properties = new SlimePropertyMap();

properties.setValue(SlimeProperties.DIFFICULTY, "normal");
properties.setValue(SlimeProperties.SPAWN_X, 123);
properties.setValue(SlimeProperties.SPAWN_Y, 112);
properties.setValue(SlimeProperties.SPAWN_Z, 170);
properties.setValue(SlimeProperties.ALLOW_ANIMALS, false);
properties.setValue(SlimeProperties.ALLOW_MONSTERS, false);
properties.setValue(SlimeProperties.DRAGON_BATTLE, false);
properties.setValue(SlimeProperties.PVP, false);
properties.setValue(SlimeProperties.ENVIRONMENT, "normal");
properties.setValue(SlimeProperties.WORLD_TYPE, "DEFAULT");
properties.setValue(SlimeProperties.DEFAULT_BIOME, "minecraft:plains");
// add as many as you would like
```

[1]: ../../api/src/main/java/com/infernalsuite/aswm/api/world/properties/SlimeProperty.java
[2]: ../../api/src/main/java/com/infernalsuite/aswm/api/world/properties/SlimeProperties.java
[3]: ../../api/src/main/java/com/infernalsuite/aswm/api/world/properties/SlimePropertyMap.java
