plugins {
    java
}

group = "network.blazemc"
version = "1.0.0"
description = "Export ItemsAdder custom items and armor to Base64 for crate configurations."

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("beer.devs:itemsadder-api:4.0.18-beta-10")

    testImplementation("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v1.21:4.110.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks.withType<Javadoc>().configureEach {
    options.encoding = "UTF-8"
    (options as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
}

tasks.processResources {
    val props = mapOf("version" to version.toString())
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStandardStreams = false
    }
}

tasks.jar {
    archiveBaseName.set("BExport")
    archiveVersion.set("")
    archiveClassifier.set("")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Implementation-Title" to "BExport",
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to "BlazeMC Network",
            "Built-By" to "BlazeMC Network",
            "Paper-Api-Version" to "1.21.11-R0.1-SNAPSHOT",
        )
    }
}

tasks.register<Copy>("distJar") {
    dependsOn(tasks.jar)
    from(tasks.jar)
    into(layout.buildDirectory.dir("dist"))
    rename { "BExport.jar" }
}

tasks.build {
    dependsOn("distJar")
}
