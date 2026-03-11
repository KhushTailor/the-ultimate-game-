pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "UltimateGameEngine"
include(
    ":engine-core",
    ":engine-rendering",
    ":engine-physics",
    ":engine-audio",
    ":engine-input",
    ":engine-network",
    ":engine-ui",
    ":assets",
    ":game-sample"
)
