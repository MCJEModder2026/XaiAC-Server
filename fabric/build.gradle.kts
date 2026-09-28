plugins {
    `multiloader-loader`
    id("net.fabricmc.fabric-loom") version "1.18-SNAPSHOT" apply false
    id("net.fabricmc.fabric-loom-remap") version "1.18-SNAPSHOT" apply false
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    `maven-publish`
}

repositories {
    maven("https://maven.fabricmc.net/")
}

dependencies {
    minecraft("com.mojang:minecraft:${commonMod.mc}")
    loomx.applyMojangMappings()
    modImplementation("net.fabricmc:fabric-loader:${commonMod.dep("fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${commonMod.dep("fabric_api")}")
    compileOnly("org.jspecify:jspecify:1.0.0")
}