@file:Suppress("UnstableApiUsage")

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
        maven {
            name = "github"
            url = uri("https://maven.pkg.github.com/storytellerF/AFS")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GPR_USER"))
                    .get()
                password = providers.gradleProperty("gpr.key")
                    .orElse(providers.environmentVariable("GPR_KEY"))
                    .get()
            }
            mavenContent {
                includeGroupAndSubgroups("com.storyteller_f.afs")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "GiantExplorer"

// 主应用模块
include(":app")
include(":giant-explorer-plugin-core")

// Plugins
include(":plugins:li:li-plugin")
include(":plugins:li:li-app")
include(":plugins:yue:yue-plugin")
include(":plugins:yue:yue-app")
