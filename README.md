# Advanced Slime World Manager [![Build Status](https://travis-ci.org/Paul19988/Advanced-Slime-World-Manager.svg?branch=feature%2F1.16)](https://travis-ci.org/Paul19988/Advanced-Slime-World-Manager)

[<img src="https://discordapp.com/assets/e4923594e694a21542a489471ecffa50.svg" alt="" height="55" />](https://discord.gg/YevvsMa)

ASWM is a fork of Slime World Manager that implements the Slime Region Format, developed by the Hypixel Dev Team. 
Its goal is to provide server administrators with an easy-to-use tool to load worlds faster and save space.

## Releases

ASWM releases can be found [here](https://github.com/Paul19988/Advanced-Slime-World-Manager/releases). More recent
releases can be found in the [Discord](https://discord.gg/YevvsMa) under the #new-builds channel.

## Using SWM in your plugin

#### Maven
```xml
<repositories>
  <repository>
    <id>rapture-snapshots</id>
    <url>https://repo.rapture.pw/repository/maven-snapshots/</url>
  </repository>
</repositories>
```

```xml
<dependencies>
  <dependency>
    <groupId>com.grinderwolf</groupId>
    <artifactId>slimeworldmanager-api</artifactId>
    <version>INSERT LATEST VERSION HERE</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

#### Gradle
```groovy
repositories {
    maven { url = 'https://repo.rapture.pw/repository/maven-releases/' }
    maven { url = 'https://repo.rapture.pw/repository/maven-snapshots/' }
}

dependencies {
    compileOnly 'com.grinderwolf:slimeworldmanager-api:INSERT LATEST VERSION HERE'
}
```

#### Javadocs

The Javadocs can be found [here](https://grinderwolf.github.io/Slime-World-Manager/apidocs/).

## Wiki Overview
 * Plugin Usage
    * [Installing ASWM](.docs/usage/install.md)
    * [Using ASWM](.docs/usage/using.md)
    * [Commands & Permissions](.docs/usage/commands-and-permissions.md)
 * Configuration
    * [Setting up the Data Sources](.docs/config/setup-data-sources.md)
    * [Converting Traditional Worlds Into the SRF](.docs/config/convert-world-to-srf.md)
    * [Configuring Worlds](.docs/config/configure-world.md)
 * SWM API
    * [Getting Started](.docs/api/setup-dev.md)
    * [World Properties](.docs/api/properties.md)
    * [Loading a World](.docs/api/load-world.md)
    * [Migrating a World](.docs/api/migrate-world.md)
    * [Importing a World](.docs/api/import-world.md)
    * [Using Custom Data Sources](.docs/api/use-data-source.md)
    * [Custom Build Preparation](.docs/api/custom-build-preparation.md)
 * Other
   * [FAQ](.docs/faq.md)

## Credits

Thanks to:
 * All the contributors that actively maintain ASWM and added features to SWM.
 * [Paul19988](https://github.com/Paul19988) - ASWM Creator.
 * [ComputerNerd100](https://github.com/ComputerNerd100) - Large Contributor & Maintainer.
 * [b0ykoe](https://github.com/b0ykoe) - Provider of build services & repositories.
 * [Owen1212055](https://github.com/Owen1212055) - Large Contributor & Maintainer.
 * [Gerolmed](https://github.com/Gerolmed) - Contributor & Maintainer.
 * [Grinderwolf](https://github.com/Grinderwolf) - The original creator.
 * [Glare](https://glaremasters.me) - Providing the original Maven repository.
 * [Minikloon](https://twitter.com/Minikloon) and all the [Hypixel](https://twitter.com/HypixelNetwork) team for developing the SRF.
 
## YourKit

YourKit supports open source projects with innovative and intelligent tools for monitoring and profiling Java and .NET applications. YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/) and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

![YourKit](https://www.yourkit.com/images/yklogo.png)
