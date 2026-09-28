plugins {
    java
    idea
    `java-library`
}

version = "${commonMod.version}+${stonecutterBuild.current.version}+${loader ?: "common"}"

base {
    archivesName = commonMod.id
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(commonProject.prop("java.version")!!)
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(java.toolchain.languageVersion.get().asInt())
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven("https://repo.spongepowered.org/repository/maven-public") { name = "Sponge" }
        }
        filter { includeGroupAndSubgroups("org.spongepowered") }
    }
    exclusiveContent {
        forRepositories(
            maven("https://maven.parchmentmc.org") { name = "ParchmentMC" },
            maven("https://maven.neoforged.net/releases") { name = "NeoForge" },
            maven("https://maven.minecraftforge.net/") { name = "MinecraftForge" }
        )
        filter { includeGroup("org.parchmentmc.data") }
    }
    maven("https://www.cursemaven.com")
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content {
            includeGroup("maven.modrinth")
        }
    }
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://maven.kikugie.dev/releases")
    maven("https://thedarkcolour.github.io/KotlinForForge/")
}

dependencies {
    annotationProcessor("com.google.auto.service:auto-service:1.1.1")
    compileOnly("com.google.auto.service:auto-service-annotations:1.1.1")
}

tasks.processResources {
    val expandProps = mapOf(
        "mod_id" to commonMod.id,
        "java_version" to commonMod.propOrNull("java.version"),
        "mod_version" to commonMod.version,
        "minecraft_version" to commonMod.mc,
        "minecraft_range_fabric" to (commonMod.depOrNull("minecraft.range.fabric") ?: commonMod.mc),
        "minecraft_range_fml" to (commonMod.depOrNull("minecraft.range.fml") ?: "[${commonMod.mc}]"),
        "fabric_loader_version" to commonMod.depOrNull("fabric_loader"),
        "fabric_api_version" to commonMod.depOrNull("fabric_api"),
        "neoforge_version" to commonMod.depOrNull("neoforge"),
        "forge_version" to commonMod.depOrNull("forge"),
        "mod_menu_version" to commonMod.depOrNull("modmenu")
    ).filterValues { it?.isNotEmpty() == true }.mapValues { (_, v) -> v!! }

    val jsonExpandProps = expandProps.mapValues { (_, v) -> v.replace("\n", "\\\\n") }

    filesMatching(listOf("META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
        expand(expandProps)
    }

    filesMatching(listOf("pack.mcmeta", "fabric.mod.json")) {
        expand(jsonExpandProps)
    }

    filesMatching(listOf("plugin.yml")) {
        expand(expandProps)
    }

    inputs.properties(expandProps)
    dependsOn(commonProject.tasks["stonecutterGenerate"])
}