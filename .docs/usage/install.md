## Installing Advanced Slime World Manager

### Releases

ASWM releases can be found [here](https://github.com/Paul19988/Advanced-Slime-World-Manager/releases). More recent
releases can be found in the [Discord](https://discord.gg/YevvsMa) under the #new-builds channel.

### How to install ASWM

Installing ASWM is an easy task. First, download the latest version of the plugin and class modifier. Then, follow this step:
1. Place the downloaded `slimeworldmanager-plugin-<version>.jar` file inside your server's plugin folder.
2. Place the `slimeworldmanager-classmodifier-<version>.jar` file inside your server's main directory **(not the plugins folder)**.
3. Modify your server startup command and at this argument before '-jar':
```
-javaagent:slimeworldmanager-classmodifier-<version>.jar
```
---
In the end, your server startup command should look something like this:
```
java -Xmx3G -javaagent:slimeworldmanager-classmodifier-<version>.jar -jar server.jar
```
