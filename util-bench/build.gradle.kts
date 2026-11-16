plugins {
    id("com.eignex.jvm")
    application
}

description = "JVM primitive collection benchmarks and eignex-lab adapter"
eignexPublish { publish.set(false) }
eignexBuild {
    jvmToolchain.set(17)
    abiValidationEnabled.set(false)
}

repositories {
    exclusiveContent {
        forRepository {
            ivy {
                name = "labBenchReleases"
                url = uri(providers.gradleProperty("labBenchRepository")
                    .getOrElse("https://github.com/Eignex/eignex-lab/releases/download"))
                patternLayout { artifact("lab-bench-v[revision]/[artifact]-[revision](-[classifier]).[ext]") }
                metadataSources { gradleMetadata() }
            }
        }
        filter { includeModule("com.eignex", "lab-bench") }
    }
}

dependencies {
    implementation(project(":util"))
    implementation("com.eignex:lab-bench:${providers.gradleProperty("labBenchVersion").getOrElse("0.1.1")}")
    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
    testImplementation(kotlin("test-junit5"))
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application { mainClass.set("com.eignex.util.bench.LabAdapterKt") }
tasks.test { useJUnitPlatform() }
