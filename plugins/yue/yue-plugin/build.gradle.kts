import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.library")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.storyteller_f.yue_plugin"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    val javaVersion = JavaVersion.forClassVersion(libs.versions.jdk.get().toInt() + 44)
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
    lint {
        targetSdk = libs.versions.compileSdk.get().toInt()
    }
}
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.versions.jdk.get())
        optIn.add("kotlin.RequiresOptIn")
    }
}
dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.fragment.ktx)
    implementation(libs.activity.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    api(project(":giant-explorer-plugin-core"))
    api(libs.lifecycle.runtime.ktx)
}

// Keep the public task/output path; Android application packaging supplies linked resources.
val packGep = tasks.register<Sync>("packGep") {
    group = "gep"
    dependsOn(":plugins:yue:yue-gep:assembleRelease")
    from(project(":plugins:yue:yue-gep").layout.buildDirectory.dir("outputs/apk/release")) {
        include("*.apk")
        rename { "yue.gep" }
    }
    into(layout.buildDirectory.dir("outputs/gep"))
    doLast {
        check(destinationDir.resolve("yue.gep").isFile) { "Missing packaged Fragment GEP" }
    }
}
