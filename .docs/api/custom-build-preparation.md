## How can I build a custom build?

In short, ASWM requires the necessary Spigot files in your local Maven repository. The easiest way to do this is to use [Spigot's build tools](https://www.spigotmc.org/wiki/buildtools/). You can find all the information you need in the Wiki.

Summary from the Wiki:
- Download and install [Git](http://msysgit.github.io/)
- Download and install [Java 17](https://adoptium.net/temurin/releases?version=17) (AdoptOpenJDK works)
- Download the [BuildTools.jar](https://hub.spigotmc.org/jenkins/job/BuildTools/lastSuccessfulBuild/artifact/target/BuildTools.jar)
- Open a terminal or shell (the Git shell also works) and navigate to the file **BuildTools.jar**
- Build the following classes, they will automatically be added to your local Maven repo
  - `java -jar BuildTools.jar --rev 1.19.2`
  - `java -jar BuildTools.jar --rev 1.19.1`
  - `java -jar BuildTools.jar --rev 1.19`
  - `java -jar BuildTools.jar --rev 1.18.2`
- Make changes to ASWM and compile it into Maven using `package`
