plugins {
    java
    application
}

val include: Configuration by configurations.creating

configurations {
    implementation.get().extendsFrom(include)
    api.get().extendsFrom(include)
}

repositories {
    maven {
        name = "Lenni0451 Releases"
        url = uri("https://maven.lenni0451.net/releases")
    }
}

dependencies {
    include(project(":"))

    include("net.lenni0451.classtransform:core:1.9.1")

    include("net.sf.jopt-simple:jopt-simple:5.0.4")

    include("org.apache.logging.log4j:log4j-core:2.20.0")
    include("org.apache.logging.log4j:log4j-slf4j2-impl:2.20.0")

    compileOnly("org.jetbrains:annotations:24.0.1")

    include("me.tongfei:progressbar:0.9.4")
}

application {
    mainClass.set("net.raphimc.javadowngrader.standalone.Main")
}

tasks.jar {
    dependsOn(include)
    from({
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        include.map {
            zipTree(it)
        }
    }, {
        exclude("META-INF/*.RSA", "META-INF/*.SF", "META-INF/*.DSA")
    })

    manifest {
        attributes["Main-Class"] = application.mainClass.get()
        attributes["Multi-Release"] = true
    }
}
