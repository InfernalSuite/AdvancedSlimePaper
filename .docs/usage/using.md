# Using Advanced Slime World Manager

## How does ASWM work?

In short, ASWM (a fork of [SWM](https://www.spigotmc.org/resources/slimeworldmanager.69974/)) is a Bukkit world manager plugin, like Multiverse or MultiWorld. However, these plugins are limited to loading traditional Minecraft worlds - and they're great at it, too. ASWM uses the Slime Region Format, which has some advantages over the traditional MC format - you can read more about this [here](https://www.spigotmc.org/resources/slimeworldmanager.69974/).

## How can I use ASWM?

ASWM needs to have worlds to load, otherwise it's useless. First of all, you have to import your world to the SRF, so ASWM can load it. To do so, check out [this page](../config/convert-world-to-srf.md).

Once you have your world imported and stored inside a data source, you've got to configure it, so the plugin knows where the world is stored, its spawn location, etc... Take a look at [this page](../config/configure-world.md) for more information on how to achieve this.

Now that you've saved and configured your world, it's time to load it. Type `/swm load <your-world-name>`. That's it! Now you can teleport yourself to the world by using the `/swm goto <your-world-name>` command.
