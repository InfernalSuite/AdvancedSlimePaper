# Development Setup

## Building the Plugin

To build ASWM, execute the following command in the project root:

```
gradle clean shadowJar
```

## Using the API


If your plugin wants to use Advanced Slime World Manager add the following in your plugin:

### Maven
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

### Gradle
```groovy
repositories {
    maven { url = 'https://repo.rapture.pw/repository/maven-snapshots/' }
}

dependencies {
    compileOnly 'com.grinderwolf:slimeworldmanager-api:INSERT LATEST VERSION HERE'
}
```

**If you run into any Flow-NBT errors when building your project, add the additional repository: `https://repo.rapture.pw/repository/maven-releases/`**
