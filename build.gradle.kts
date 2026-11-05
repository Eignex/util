import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("com.eignex.kmp")
    id("com.eignex.publish.github")
}

repositories {
    ivy {
        url = uri("https://github.com/Eignex/kbuild/releases/download")
        patternLayout { artifact("v[revision]/[artifact]-[revision](-[classifier]).[ext]") }
        metadataSources { gradleMetadata() }
        content { includeModule("com.eignex", "kbuild-platform") }
    }
}

eignexPublish {
    description.set(
        "Shared Kotlin Multiplatform utilities: primitive collections, array helpers, " +
                "concurrency primitives, and platform support."
    )
    githubRepo.set("Eignex/util")
}

// Routine checks cover JVM + Linux x64; release validation opts into every target.
val fullTargets = providers.gradleProperty("targets.full").isPresent

eignexBuild {
    jvmToolchain.set(17)
    abiValidationEnabled.set(fullTargets)
}

@OptIn(ExperimentalWasmDsl::class)
kotlin {
    compilerOptions.freeCompilerArgs.add("-Xexpect-actual-classes")
    jvm()
    linuxX64()
    if (fullTargets) {
        js { browser(); nodejs() }
        wasmJs { browser(); nodejs() }
        wasmWasi { nodejs() }
        linuxArm64()
        macosArm64(); mingwX64()
        iosX64(); iosArm64(); iosSimulatorArm64()
    }

    applyDefaultHierarchyTemplate()
    sourceSets {
        val hostMain = create("hostMain") { dependsOn(commonMain.get()) }
        jvmMain.get().dependsOn(hostMain)
        nativeMain.get().dependsOn(hostMain)
        val hostTest = create("hostTest") { dependsOn(commonTest.get()) }
        jvmTest.get().dependsOn(hostTest)
        nativeTest.get().dependsOn(hostTest)

        val posixMain = create("posixMain") { dependsOn(nativeMain.get()) }
        linuxMain.get().dependsOn(posixMain)
        if (fullTargets) {
            appleMain.get().dependsOn(posixMain)
            val singleThreadMain = create("singleThreadMain") { dependsOn(commonMain.get()) }
            jsMain.get().dependsOn(singleThreadMain)
            wasmJsMain.get().dependsOn(singleThreadMain)
            wasmWasiMain.get().dependsOn(singleThreadMain)
        }
    }
}

dokka {
    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory.set(projectDir.resolve("src"))
            val sub = projectDir.relativeTo(rootDir).invariantSeparatorsPath
            val prefix = if (sub.isEmpty()) "src" else "$sub/src"
            remoteUrl("https://github.com/Eignex/${rootProject.name}/blob/main/$prefix")
            remoteLineSuffix.set("#L")
        }
    }
}

// Select an installed simulator in CI rather than relying on a particular iPhone model.
tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest>().configureEach {
    providers.gradleProperty("iosSimulatorDevice").orNull?.let { device.set(it) }
}
