import com.storyteller_f.jksify.getenv
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven {
            name = "github"
            url = uri("https://maven.pkg.github.com/storytellerF/jksify")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: ""
                password = project.findProperty("gpr.key") as String? ?: ""
            }
            mavenContent {
                includeGroupAndSubgroups("com.storyteller_f.jksify")
            }
        }
    }
    dependencies {
        classpath(libs.com.storyteller.f.song.gradle.plugin)
        classpath(libs.jksify.gradle.plugin)
    }
}

apply(plugin = "com.storyteller_f.jksify")
apply(plugin = "com.storyteller_f.song")

plugins {
    id("com.android.application")
    id("androidx.navigation.safeargs.kotlin")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.storyteller_f.yue"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.storyteller_f.yue"
        minSdk = libs.versions.minSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        val signPath: String? = getenv("storyteller_f_sign_path")
        val signKey: String? = getenv("storyteller_f_sign_key")
        val signAlias: String? = getenv("storyteller_f_sign_alias")
        val signStorePassword: String? = getenv("storyteller_f_sign_store_password")
        val signKeyPassword: String? = getenv("storyteller_f_sign_key_password")
        val signStorePath = when {
            signPath != null -> File(signPath)
            signKey != null -> layout.buildDirectory.file("signing/signing_key.jks").get().asFile
            else -> null
        }
        if (signStorePath != null && signAlias != null && signStorePassword != null && signKeyPassword != null) {
            create("release") {
                keyAlias = signAlias
                keyPassword = signKeyPassword
                storeFile = signStorePath
                storePassword = signStorePassword
            }
        }
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSignConfig = signingConfigs.findByName("release")
            if (releaseSignConfig != null)
                signingConfig = releaseSignConfig
        }
    }
    val javaVersion = JavaVersion.forClassVersion(libs.versions.jdk.get().toInt() + 44)
    compileOptions {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    buildFeatures {
        viewBinding = true
    }
    lint {
        targetSdk = libs.versions.compileSdk.get().toInt()
    }
}
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(libs.versions.jdk.get())
    }
}
dependencies {
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(project(":plugins:yue:yue-plugin"))
}

val buildPath: String = layout.buildDirectory.asFile.get().absolutePath
song {
    transfers.set(listOf("$buildPath/intermediates/apk/debug/app-debug.apk"))
    adb.set(androidComponents.sdkComponents.adb.get().asFile.absolutePath)
    paths.set(listOf())
    packages.set(listOf("com.storyteller_f.giant_explorer" to "files/plugins"))
    outputName.set("yue.apk")
}