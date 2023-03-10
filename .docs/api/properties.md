# Properties API

Property "types" are handled by [SlimeProperty][1] instances. Whilst creating [SlimeProperty][1] objects is not allowed, there is a [list of all available properties][2]. Properties and their values are stored in [SlimePropertyMaps][3].


**Example Usage:**
```java
// create a new and empty property map
SlimePropertyMap properties = new SlimePropertyMap();

properties.setString(SlimeProperties.DIFFICULTY, "normal");
properties.setInt(SlimeProperties.SPAWN_X, 123);
properties.setInt(SlimeProperties.SPAWN_Y, 112);
properties.setInt(SlimeProperties.SPAWN_Z, 170);
// add as many as you would like
```

[1]: ../../api/src/main/java/com/infernalsuite/aswm/api/world/properties/SlimeProperty.java
[2]: ../../api/src/main/java/com/infernalsuite/aswm/api/world/properties/SlimeProperties.java
[3]: ../../api/src/main/java/com/infernalsuite/aswm/api/world/properties/SlimePropertyMap.java
