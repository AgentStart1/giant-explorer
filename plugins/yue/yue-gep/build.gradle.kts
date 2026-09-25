plugins {
    id("com.android.application")
}

android {
    namespace = "com.storyteller_f.yue.gep"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.storyteller_f.yue.gep"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.compileSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            // Plugin entries are reached through reflection in the host.
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation(project(":plugins:yue:yue-plugin"))
}
