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

        jcenter()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://jcenter.bintray.com") }
        maven { url = uri("https://repo1.maven.org/maven2/") }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()

        jcenter()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://jcenter.bintray.com") }
        maven { url = uri("https://repo1.maven.org/maven2/") }
    }
}

rootProject.name = "BookElectrone"
include(":app")
 