import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.storyteller_f.li.plugin"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.minSdk.get().toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(project(":giant-explorer-plugin-core"))
}

val isWindows = System.getProperty("os.name").lowercase().startsWith("win")
val gepWorkDirectory = layout.buildDirectory.dir("intermediates/gep")
val gepClassesJar = gepWorkDirectory.map { it.file("classes.jar") }
val pluginCoreClassesJar = project(":giant-explorer-plugin-core").layout.buildDirectory.file(
    "intermediates/compile_library_classes_jar/release/bundleLibCompileToJarRelease/classes.jar"
)
val gepClasspath = providers.provider {
    val configuration = configurations.getByName("releaseCompileClasspath")
    configuration.incoming.artifactView {
        attributes.attribute(
            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
            ArtifactTypeDefinition.JAR_TYPE
        )
    }.files
}

val unpackAar = tasks.register<Copy>("unpackAar") {
    group = "gep"
    dependsOn("bundleReleaseAar")
    val aarFile = layout.buildDirectory.file("outputs/aar/li-plugin-release.aar")
    doFirst {
        delete(gepWorkDirectory)
    }
    from(zipTree(aarFile))
    into(gepWorkDirectory)
}

val convertJarToDex = tasks.register<Exec>("convertJarToDex") {
    group = "gep"
    dependsOn(unpackAar, ":giant-explorer-plugin-core:bundleLibCompileToJarRelease")

    val sdkDirectory = androidComponents.sdkComponents.sdkDirectory.get().asFile
    val d8Name = if (isWindows) "d8.bat" else "d8"
    val d8Path = File(sdkDirectory, "build-tools/${android.buildToolsVersion}/$d8Name")
    doFirst {
        val platformPrefix = "android-${android.compileSdk}"
        val androidJar = File(sdkDirectory, "platforms").listFiles()
            .orEmpty()
            .filter { it.name == platformPrefix || it.name.startsWith("$platformPrefix.") }
            .map { File(it, "android.jar") }
            .firstOrNull { it.isFile }
            ?: error("Android platform JAR not found for API ${android.compileSdk}")
        check(d8Path.isFile) { "D8 not found: ${d8Path.absolutePath}" }
        check(androidJar.isFile) { "Android platform JAR not found: ${androidJar.absolutePath}" }
        val workDirectory = gepWorkDirectory.get().asFile
        workingDir = workDirectory
        val arguments = mutableListOf(
            "--lib", androidJar.absolutePath,
            "--output", workDirectory.absolutePath,
            gepClassesJar.get().asFile.absolutePath
        )
        (gepClasspath.get().filter { it.isFile } + pluginCoreClassesJar.get().asFile)
            .distinctBy { it.absolutePath }
            .forEach { file ->
                arguments.addAll(listOf("--classpath", file.absolutePath))
            }
        val argumentFile = File(workDirectory, "d8-arguments.txt")
        argumentFile.writeText(
            arguments.joinToString(System.lineSeparator()) { argument ->
                if (argument.any(Char::isWhitespace)) "\"${argument.replace("\\", "/")}\"" else argument
            }
        )
        commandLine(d8Path.absolutePath, "@${argumentFile.absolutePath}")
    }
}

val packGep = tasks.register<Zip>("packGep") {
    group = "gep"
    dependsOn(convertJarToDex)

    archiveFileName.set("li.gep")
    destinationDirectory.set(layout.buildDirectory.dir("outputs/gep"))
    exclude("*.jar", "d8-arguments.txt")
    from(gepWorkDirectory)
    from({ zipTree(gepClassesJar.get().asFile) }) {
        exclude("**/*.class")
    }
}

tasks.build {
    finalizedBy(packGep)
}
