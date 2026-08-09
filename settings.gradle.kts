@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { setUrl("https://jitpack.io") }
        maven {
            name = "github"
            url = uri("https://maven.pkg.github.com/storytellerF/jksify")
            credentials {
                // 需要配置在~/.gradle/gradle.properties
                username = providers.gradleProperty("gpr.user").get()
                password = providers.gradleProperty("gpr.key").get()
            }
            mavenContent {
                includeGroupAndSubgroups("com.storyteller_f.jksify")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven {
            name = "github"
            url = uri("https://maven.pkg.github.com/storytellerF/common-ui-list")
            credentials {
                // 需要配置在~/.gradle/gradle.properties
                username = providers.gradleProperty("gpr.user").get()
                password = providers.gradleProperty("gpr.key").get()
            }
            mavenContent {
                includeGroupAndSubgroups("com.storyteller_f.common_ui_list")
                includeGroupAndSubgroups("com.storytellerF.common_ui_list")
            }
        }
        maven {
            name = "github"
            url = uri("https://maven.pkg.github.com/storytellerF/AFS")
            credentials {
                // 需要配置在~/.gradle/gradle.properties
                username = providers.gradleProperty("gpr.user").get()
                password = providers.gradleProperty("gpr.key").get()
            }
            mavenContent {
                includeGroupAndSubgroups("com.storyteller_f.afs")
            }
        }
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        //maven { setUrl("https://artifactory.cronapp.io/public-release/") }
    }
}

rootProject.name = "GiantExplorer"

// 主应用模块
include(":giant-explorer")
include(":giant-explorer-plugin-core")

// Plugins (optional - only include if directories exist)
file("plugins/li/li-plugin").takeIf { it.isDirectory }?.let {
    include(":plugins:li:li-plugin")
}
file("plugins/li/li-app").takeIf { it.isDirectory }?.let {
    include(":plugins:li:li-app")
}
file("plugins/yue/yue-plugin").takeIf { it.isDirectory }?.let {
    include(":plugins:yue:yue-plugin")
}
file("plugins/yue/yue-app").takeIf { it.isDirectory }?.let {
    include(":plugins:yue:yue-app")
}
