# JavaDowngrader
Standalone program and library which can downgrade Java classes/programs down to Java 8.

To use JavaDowngrader as a library in your application, check out the [Usage (As a library)](#usage-as-a-library) section.  
If you just want to downgrade .jar files you can check out the [Usage (Standalone)](#usage-standalonebootstrap) section.

## Features
- Supports up to Java 22 and down to Java 8
- Downgrades language features
- Downgrades important Java API calls
- Can be applied during runtime
- Can downgrade .jar files

## Releases
### Executable Jar File
If you want the executable jar file you can download a stable release from [GitHub Releases](https://github.com/RaphiMC/JavaDowngrader/releases/latest) or the latest dev version from [GitHub Actions](https://github.com/RaphiMC/JavaDowngrader/actions/workflows/build.yml) or [Lenni0451's Jenkins](https://build.lenni0451.net/job/JavaDowngrader/).

### Gradle/Maven
To use JavaDowngrader with Gradle/Maven you can get it from [Maven Central](https://mvnrepository.com/artifact/net.raphimc.javadowngrader), [Lenni0451's Maven](https://maven.lenni0451.net/#/releases/net/raphimc/javadowngrader) or [Jitpack](https://jitpack.io/#RaphiMC/JavaDowngrader).
You can also find instructions how to implement it into your build script there.

## Usage (Standalone/Bootstrap)
1. Download the latest version from the [GitHub Releases](#executable-jar-file)
2. Run the jar file with `java -jar JavaDowngrader-whateverversion.jar` to see the usage

### Downgrade ahead of time
Here is an example command to convert the jar input.jar to Java 8 and output it to output.jar:
``java -jar JavaDowngrader-Standalone-whateverversion.jar -i "input.jar" -o "output.jar" -v 8``

### Downgrade during runtime
Here is an example command to run a jar file and downgrade it during runtime by using the bootstrap java agent:
``java -javaagent:JavaDowngrader-Bootstrap-whateverversion.jar -jar the_real_jar_you_want_to_run.jar``

Additionally, you can set the ``-DspoofJavaVersion=<java version>`` property to bypass application specific checks for the Java version.

## Usage (As a library)
To transform a ``ClassNode`` you can use the ``JavaDowngrader`` class.  
As a low level class modification framework in your application [ClassTransform](https://github.com/Lenni0451/ClassTransform) is recommended.
JavaDowngrader provides the ``impl-classtransform`` submodule which contains various utility classes for ClassTransform.

## Usage (In Gradle)
To use JavaDowngrader in Gradle you can get it from [Lenni0451's Maven](https://maven.lenni0451.net/#/releases/net/raphimc/java-downgrader).
You can also find instructions how to implement it into your build script there.

After adding the repository to your ``settings.gradle`` file you can apply the plugin like this:
```groovy
plugins {
    id "net.raphimc.java-downgrader" version "x.x.x"
}
```

### Downgrade the main source set
```groovy
import net.raphimc.javadowngrader.gradle.task.DowngradeSourceSetTask

tasks.register("java8Main", DowngradeSourceSetTask) {
    sourceSet = sourceSets.main
}.get().dependsOn("classes")
classes.finalizedBy("java8Main")
```

### Downgrade the built jar (If you use Java 8+ libraries)
```groovy
import net.raphimc.javadowngrader.gradle.task.DowngradeJarTask

tasks.register("java8Jar", DowngradeJarTask) {
    input = tasks.jar.archiveFile.get().asFile
    outputSuffix = "+java8"
    compileClassPath = sourceSets.main.compileClasspath
}.get().dependsOn("build")
build.finalizedBy("java8Jar")
```

Some of the optional properties include:
- ``targetVersion``: The target classfile version (Default: 8)
- ``outputSuffix``: The suffix to append to the output jar file (Default: "-downgraded")
- ``copyRuntimeClasses``: Whether to copy the JavaDowngrader runtime classes to the output jar (Default: true). Should be set to false if your jar already contains JavaDowngrader itself

## Usage (In Maven)
JavaDowngrader doesn't have official maven support, but there is an [unofficial maven plugin made by tr7zw](https://github.com/tr7zw/JavaDowngrader-Maven).
You can find instructions on how to implement it into your build script there.

## Contact
If you encounter any issues, please report them on the
[issue tracker](https://github.com/RaphiMC/JavaDowngrader/issues).  
If you just want to talk or need help using JavaDowngrader feel free to join my
[Discord](https://discord.gg/dCzT9XHEWu).
