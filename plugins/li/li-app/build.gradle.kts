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
        classpath(libs.jksify.gradle.plugin)
    }
}

apply(plugin = "com.storyteller_f.jksify")

plugins {
    id("com.android.application")
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.storyteller_f.li"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.storyteller_f.li"
        minSdk = libs.versions.minSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
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
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    dependenciesInfo {
        includeInBundle = false
        includeInApk = false
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

    debugImplementation(libs.leakcanary.android)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.navigation.ui.ktx)
    implementation(libs.constraintlayout)
    implementation(project(":plugins:li:li-plugin"))
}
