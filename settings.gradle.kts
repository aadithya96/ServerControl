pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
// toolchain auto-resolver disabled in offline/restricted environments
// plugins {
//     id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
// }

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "ServerControl"
include(":app")
