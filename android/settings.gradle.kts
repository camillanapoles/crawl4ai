// ──────────────────────────────────────────────────────────────────────────
//  Crawl4AI Android — Settings
// ──────────────────────────────────────────────────────────────────────────
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        // Chaquopy plugin repository
        maven("https://chaquo.com/maven")
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://chaquo.com/maven")
        mavenCentral()
        // Markdown rendering
        maven("https://jitpack.io")
    }
    versionCatalogs {
        create("libs") {
            from(files("gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "Crawl4AI"
include(":app")
