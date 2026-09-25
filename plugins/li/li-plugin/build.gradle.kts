import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.gradle.process.ExecOperations
import javax.inject.Inject

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
val gepWorkDirectory = layout.buildDirectory.dir("intermediates/gep/aar")
val gepClassesJar = gepWorkDirectory.map { it.file("classes.jar") }
val pluginCoreClassesJar = project(":giant-explorer-plugin-core").layout.buildDirectory.file(
    "intermediates/compile_library_classes_jar/release/bundleLibCompileToJarRelease/classes.jar"
)
val gepClasspath = providers.provider {
    configurations.getByName("releaseCompileClasspath").incoming.artifactView {
        attributes.attribute(
            ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE,
            ArtifactTypeDefinition.JAR_TYPE
        )
    }.files
}

val unpackAar = tasks.register<Sync>("unpackAar") {
    group = "gep"
    dependsOn("bundleReleaseAar")
    val aarFile = layout.buildDirectory.file("outputs/aar/li-plugin-release.aar")
    from(zipTree(aarFile))
    into(gepWorkDirectory)
}

abstract class GepDexTask : DefaultTask() {
    @get:Classpath abstract val classesJar: RegularFileProperty
    @get:Classpath abstract val compileClasspath: ConfigurableFileCollection
    @get:Classpath abstract val androidClasspath: ConfigurableFileCollection
    @get:InputDirectory @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val buildToolsDirectory: DirectoryProperty
    @get:Input abstract val executableName: Property<String>
    @get:OutputDirectory abstract val dexDirectory: DirectoryProperty
    @get:Inject abstract val execOperations: ExecOperations
    @get:Inject abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun convert() {
        val workDirectory = dexDirectory.get().asFile
        fileSystemOperations.delete { delete(workDirectory) }
        check(workDirectory.mkdirs()) { "Cannot create DEX directory: $workDirectory" }
        val arguments = mutableListOf(
            "--output", workDirectory.absolutePath,
            classesJar.get().asFile.absolutePath
        )
        androidClasspath.files.forEach { arguments.addAll(listOf("--lib", it.absolutePath)) }
        compileClasspath.files
            .distinctBy { it.absolutePath }
            .forEach { file ->
                arguments.addAll(listOf("--classpath", file.absolutePath))
            }
        val argumentFile = File(temporaryDir, "d8-arguments.txt")
        argumentFile.writeText(
            arguments.joinToString(System.lineSeparator()) { argument ->
                if (argument.any(Char::isWhitespace)) "\"${argument.replace("\\", "/")}\"" else argument
            }
        )
        execOperations.exec {
            commandLine(buildToolsDirectory.file(executableName).get().asFile.absolutePath, "@${argumentFile.absolutePath}")
        }
    }
}

val convertJarToDex = tasks.register<GepDexTask>("convertJarToDex") {
    group = "gep"
    dependsOn(unpackAar, ":giant-explorer-plugin-core:bundleLibCompileToJarRelease")
    classesJar.set(gepClassesJar)
    compileClasspath.from(gepClasspath, pluginCoreClassesJar)
    androidClasspath.from(androidComponents.sdkComponents.bootClasspath)
    val buildToolsVersion = android.buildToolsVersion
    buildToolsDirectory.set(androidComponents.sdkComponents.sdkDirectory.map {
        it.dir("build-tools/$buildToolsVersion")
    })
    executableName.set(if (isWindows) "d8.bat" else "d8")
    dexDirectory.set(layout.buildDirectory.dir("intermediates/gep/dex"))
}

val packGep = tasks.register<Zip>("packGep") {
    group = "gep"
    dependsOn(convertJarToDex)

    archiveFileName.set("li.gep")
    destinationDirectory.set(layout.buildDirectory.dir("outputs/gep"))
    exclude("*.jar", "d8-arguments.txt")
    from(gepWorkDirectory)
    from(convertJarToDex.flatMap { it.dexDirectory })
    from(zipTree(gepClassesJar)) {
        exclude("**/*.class")
    }
}

tasks.build {
    finalizedBy(packGep)
}
