plugins {
    java
    idea
    id("multiloader-shared")
}

val loaderCommonJava = configurations.create("loaderCommonJava") {
    isCanBeResolved = true
}
val loaderCommonResources = configurations.create("loaderCommonResources") {
    isCanBeResolved = true
}

dependencies {
    compileOnly(project(path = commonProject.path))
    loaderCommonJava(project(path = commonProject.path, configuration = "commonJava"))
    loaderCommonResources(project(path = commonProject.path, configuration = "commonResources"))
}
tasks {
    compileJava {
        dependsOn(loaderCommonJava)
        source(loaderCommonJava)
    }

    processResources {
        dependsOn(loaderCommonResources)
        from(loaderCommonResources)
    }
}