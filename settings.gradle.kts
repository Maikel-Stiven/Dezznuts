pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Solo para NewPipeExtractor (YouTube Music); el resto de grupos sale de Google y Maven Central.
        maven("https://jitpack.io") { content { includeGroup("com.github.TeamNewPipe") } }
    }
}

rootProject.name = "C reproductor"
include(":app")
 