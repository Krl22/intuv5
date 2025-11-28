import java.util.Properties

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
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Mapbox downloads repository (requires MAPBOX_DOWNLOADS_TOKEN in local.properties)
        val localProps = Properties().apply {
            val file = File(rootDir, "local.properties")
            if (file.exists()) file.inputStream().use { load(it) }
        }
        val downloadsToken = localProps.getProperty("MAPBOX_DOWNLOADS_TOKEN") ?: ""
        if (downloadsToken.isNotEmpty()) {
            maven {
                url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
                isAllowInsecureProtocol = false
                credentials {
                    username = "mapbox"
                    password = downloadsToken
                }
            }
        }
    }
}

rootProject.name = "Intu"
include(":app")
 
