plugins {
    id("com.eignex.kmp")
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
        implementation(project(":util"))
    }
}

val fixture = layout.projectDirectory.file("src/commonTest/resources/utf8.txt").asFile.absolutePath
tasks.withType<Test>().configureEach {
    environment("UTIL_CONSUMER_FIXTURE", fixture)
}
tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest>().configureEach {
    environment("UTIL_CONSUMER_FIXTURE", fixture)
}
