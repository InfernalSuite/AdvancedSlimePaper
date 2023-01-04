## FAQ

### Which Spigot versions is this compatible with?

ASWM is not compatible with spigot, as it is its own fork.

### Can I override the default world?

Yes, you can!

### My server stops when booting up with a 'Failed to find ClassModifier classes' error.

Go to [Installing Slime World Manager](../usage/install.md) and follow the steps described there.

### Is ASWM compatible with Multiverse-Core?

Multiverse-Core detects ASWM worlds as unloaded, as it cannot find the world directory, and then just ignores them. There should not be any issues; however, Multiverse-Core commands will not work with ASWM worlds.

### What's the world size limit?

The Slime Region Format can handle up a 46340x4630 chunk area. That's the maximum size that SWM can _theoretically_ handle, given enough memory. However, having a world so big is not recommended at all.

There's not a specific value that you shouldn't exceed _- except for the theoretical limit, of course_. ASWM keeps a copy of all the chunks loaded in memory until the world is unloaded, so the more chunks you have, the bigger the ram usage is. How far you want to go depends on how much ram you are willing to let ASWM use. Moreover, the ram usage per chunk isn't a constant value, as it depends on the actual data stored in the chunk.
