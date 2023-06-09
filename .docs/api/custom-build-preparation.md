## How can I build a custom build?

ASWM uses modern paperweight tooling inorder to interact with the server source.

To compile ASWM, you need JDK 17 and an internet connection.

Clone this repo, run `./gradlew applyPatches`, then `./gradlew createReobfBundlerJar` from your terminal. You can find the compiled jar in the project root's build/libs directory.
To get a full list of tasks, run `./gradlew tasks`.

## Building the Plugin

To build the ASWM plugin, execute the following command in the plugin module:

```
gradle clean shadowJar
```
