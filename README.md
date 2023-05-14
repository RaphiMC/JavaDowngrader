# JavaDowngrader
Standalone program and library which can downgrade Java classes/programs down to Java 8.

To use JavaDowngrader as a library in your application, check out the [Usage (As a library)](#usage-as-a-library) section.  
If you just want to downgrade .jar files you can check out the [Usage (Standalone)](#usage-standalone) section.

## Features
- Supports up to Java 21 and down to Java 8
- Downgrades language features
- Downgrades important Java API calls
- Can be applied during runtime
- Can downgrade .jar files

## Releases
### Executable Jar File
If you want the executable jar file you can download a stable release from [GitHub](https://github.com/RaphiMC/JavaDowngrader/releases/latest) or the latest dev version from this [Jenkins](https://build.lenni0451.net/job/JavaDowngrader/).

### Gradle/Maven
To use JavaDowngrader with Gradle/Maven you can use this [Maven server](https://maven.lenni0451.net/#/releases/net/raphimc/JavaDowngrader) or [Jitpack](https://jitpack.io/#RaphiMC/JavaDowngrader).  
You can also find instructions how to implement it into your build script there.

## Usage (Standalone)
1. Download the latest version from the [Releases section](#executable-jar-file)
2. Run the jar file with `java -jar JavaDowngrader-whateverversion.jar` to see the usage

Here is an example command to convert the jar input.jar to Java 8 and output it to output.jar:
``java -jar JavaDowngrader-whateverversion.jar -i "input.jar" -o "output.jar" -v 8``

## Usage (As a library)
To transform a ``ClassNode`` you can use the ``JavaDowngrader`` class.  
As a low level class modification framework in your application [ClassTransform](https://github.com/Lenni0451/ClassTransform) is recommended.

## Contact
If you encounter any issues, please report them on the
[issue tracker](https://github.com/RaphiMC/JavaDowngrader/issues).  
If you just want to talk or need help using JavaDowngrader feel free to join my
[Discord](https://discord.gg/dCzT9XHEWu).
