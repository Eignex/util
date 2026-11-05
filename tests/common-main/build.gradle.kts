plugins {
    id("com.eignex.kmp")
}

repositories {
    ivy {
        url = uri("https://github.com/Eignex/kbuild/releases/download")
        patternLayout { artifact("v[revision]/[artifact]-[revision](-[classifier]).[ext]") }
        metadataSources { gradleMetadata() }
        content { includeModule("com.eignex", "kbuild-platform") }
    }
}

description = "Cross-module contracts for consumers with commonMain production sources."

eignexPublish {
    publish.set(false)
}

eignexBuild {
    abiValidationEnabled.set(false)
}

kotlin {
    jvm()
    linuxX64()
    if (providers.gradleProperty("targets.full").isPresent) {
        linuxArm64()
        macosArm64()
    }
    sourceSets.commonMain.dependencies {
        implementation(project(":"))
    }
}

val fixture = layout.projectDirectory.file("src/commonTest/resources/utf8.txt").asFile.absolutePath
tasks.withType<Test>().configureEach {
    environment("UTIL_CONSUMER_FIXTURE", fixture)
}
tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest>().configureEach {
    environment("UTIL_CONSUMER_FIXTURE", fixture)
}
