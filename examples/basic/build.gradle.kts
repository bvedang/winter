plugins {
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(project(":core"))
}

application {
    mainClass.set("winter.example.basic.Main")
}

tasks.named<JavaExec>("run") {
    workingDir = projectDir
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}
