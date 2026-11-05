plugins { java }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(17)) }
repositories { mavenCentral() }
dependencies {
    implementation(files(providers.gradleProperty("utilJar").get()))
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.4.21")
    implementation("org.openjdk.jmh:jmh-core:1.37")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.37")
}
tasks.register<JavaExec>("benchmark") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(17)) })
    mainClass.set("org.openjdk.jmh.Main")
    args("CollectionsBench", "-wi", "3", "-w", "500ms", "-i", "5", "-r", "500ms", "-f", "2",
        "-rf", "json", "-rff", providers.gradleProperty("result").get())
}

val benchmarkClasspath = sourceSets.main.get().runtimeClasspath
val classpathFile = layout.buildDirectory.file("benchmark-classpath.txt")
tasks.register("writeBenchmarkClasspath") {
    dependsOn(tasks.classes)
    inputs.files(benchmarkClasspath)
    outputs.file(classpathFile)
    doLast { classpathFile.get().asFile.writeText(benchmarkClasspath.asPath) }
}
