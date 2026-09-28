plugins {
    `multiloader-loader`
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    mavenCentral()
    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        minecraftVersion(commonMod.dep("minecraft"))
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${commonMod.dep("paper")}")
    compileOnly("org.jspecify:jspecify:1.0.0")
}