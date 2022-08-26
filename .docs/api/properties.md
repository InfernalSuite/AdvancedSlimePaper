# Properties API

Property "types" are handled by [SlimeProperty][1] instances. Whilst creating [SlimeProperty][1] objects is not allowed, there is a [list of all available properties][2]. Properties and their values are stored in [SlimePropertyMaps][3].


**Example Usage:**
```java
// Create a new and empty property map
SlimePropertyMap properties = new SlimePropertyMap();

properties.setString(SlimeProperties.DIFFICULTY, "normal");
properties.setInt(SlimeProperties.SPAWN_X, 123);
properties.setInt(SlimeProperties.SPAWN_Y, 112);
properties.setInt(SlimeProperties.SPAWN_Z, 170);
/* Add as many as you like */
```


[1]: ../../slimeworldmanager-api/src/main/java/com/grinderwolf/swm/api/world/properties/SlimeProperty.java
[2]: ../../slimeworldmanager-api/src/main/java/com/grinderwolf/swm/api/world/properties/SlimeProperties.java
[3]: ../../slimeworldmanager-api/src/main/java/com/grinderwolf/swm/api/world/properties/SlimePropertyMap.java
