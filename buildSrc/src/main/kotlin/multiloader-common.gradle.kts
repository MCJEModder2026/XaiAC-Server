plugins {
    id("multiloader-shared")
}

val commonJava = project.configurations.create("commonJava") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

val commonResources = project.configurations.create("commonResources") {
    isCanBeResolved = false
    isCanBeConsumed = true
}

artifacts {
    afterEvaluate {
        val mainSourceSet = sourceSets.main.get()

        mainSourceSet.java.sourceDirectories.files.forEach {
            add(commonJava.name, it)
        }

        mainSourceSet.resources.sourceDirectories.files.forEach {
            add(commonResources.name, it)
        }
    }
}