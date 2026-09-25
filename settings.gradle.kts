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
include(":plugins:yue:yue-gep")
