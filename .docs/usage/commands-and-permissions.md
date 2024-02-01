# Commands & Permissions

This pages contains all the commands inside ASWM, alongside their respective permissions and usages. Arguments within angle brackets are required, and the ones within square brackets are optional.

The `swm.*` permission will grant access to all commands.

## General commands

### /swm help
Permission required: none.<br>
Usage: `/swm help`<br>
Description: Shows the plugin's help page, containing only the commands you have access to.

### /swm debug
Permission required: `swm.debug`<br>
Usage: `/swm debug`<br>
Description: Toggles debug messages.

### /swm version
Permission required: none.<br>
Usage: `/swm version`<br>
Description: Shows the plugin version.

### /swm reload
Permission required: `swm.reload`
Usage: `/swm reload`<br>
Since version: 1.1.0.<br>
Description: Reloads the config files.

### /swm goto
Permission required: `swm.goto`<br>
Usage: `/swm goto <world> [player]`<br>
Description: Teleports yourself to a world. If you want to teleport someone else, you can specify it by using the _player_ argument. 

**This command also works for traditional worlds, not just SRF worlds.**

## World Listing commands

### /swm list
Permission required: `swm.worldlist`<br>
Usage: `/swm list [slime] [page]`<br>
Description: Lists all worlds, including loaded non-SRF worlds. If you use the `slime` argument, only SRF worlds will be shown.

### /swm dslist
Permission required: `swm.dslist`<br>
Usage: `/swm list <data-source> [page]`<br>
Since version: 2.0.0.<br>
Description: Lists all worlds contained inside a specified data source. Note that this command doesn't just list the worlds that are inside the config file, but every world inside the data source.

## World Creation and Loading commands

### /swm import
Permission required: `swm.importworld`<br>
Usage: `/swm import <path-to-world> <data-source> [new-world-name]`<br>
Since version: 1.1.0.<br>
Description: Converts a world into the SRF and stores it inside the provided data source. You can check out [this page](https://github.com/Grinderwolf/Slime-World-Manager/wiki/Converting-traditional-worlds-into-the-SRF) for more information on how to use this command.

### /swm load
Permission required: `swm.loadworld`<br>
Usage: `/swm load <world>`<br>
Description: Loads a world from the config file. Remember to configure the world after converting it to the SRF. More on that [here](https://github.com/Grinderwolf/Slime-World-Manager/wiki/Configuring-worlds).

### /swm load-template
Permission required: `swm.loadworld.template`<br>
Usage: `/swm load-template <template-world> <world>`<br>
Since version: 2.0.0.<br>
Description: Creates a clone of the provided template world. This can be used to create many copies of the same world. 

**Cloned template worlds are temporary, and they will never be actually stored anywhere, so any changes to them will be lost once the server is shut down.**

### /swm clone
Permission required: `swm.cloneworld`<br>
Usage: `/swm clone <template-world> <world> [new-data-source]`<br>
Since version: 2.2.0.<br>
Description: Creates a clone of the provided template world. If not provided, SWM will use the data source of the template world to save the clone.

### /swm create
Permission required: `swm.createworld`<br>
Usage: `/swm create <world> <data-source>`<br>
Since version: 1.1.0.<br>
Description: Creates an empty world and stores it in the provided data source. This command will also automatically save the world in the config file.

### /swm unload
Permission required: `swm.unloadworld`<br>
Usage: `/swm unload <world>`<br>
Description: Unloads a world from the server.

**This command also works for traditional worlds, not just SRF worlds.**

## World Management commands

### /swm migrate
Permission required: `swm.migrate`<br>
Usage: `/swm migrate <world> <new-data-source>`<br>
Description: Transfers a world from the current data source it's stored in to the specified.

### /swm save
Permission required: `swm.saveworld`<br>
Usage: `/swm save <world>`<br>
Description: Saves a world.

### /swm setspawn
Permission required: `swm.setspawn`<br>
Usage: `/swm setspawn <world> <x> <y> <z> <yaw> <pitch>`<br>
Description: Set the spawnpoint of a world based on your location or one provided.

### /swm delete
Permission required: `swm.deleteworld`<br>
Usage: `/swm delete <world> [data-source]`<br>
Description: Completely deletes a world. If a data source is not provided, the one specified in the config file will be used.

**This action is permanent, and there's no way to go back once the world is deleted.** To make sure you are not doing this by mistake, you'll have to type the command twice.
