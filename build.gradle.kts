plugins {
    // Subprojects share the Kotlin plugins and their build services.
    id("com.eignex.kmp") apply false
    id("com.eignex.jvm") apply false
    id("com.eignex.publish.github") apply false
}

allprojects {
    repositories {
        ivy {
            url = uri("https://github.com/Eignex/kbuild/releases/download")
            patternLayout { artifact("v[revision]/[artifact]-[revision](-[classifier]).[ext]") }
            metadataSources { gradleMetadata() }
            content { includeModule("com.eignex", "kbuild-platform") }
        }
        mavenCentral()
    }
}
