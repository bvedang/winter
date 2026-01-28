plugins {
    id("com.diffplug.spotless") version "8.2.1"
}

allprojects {
    group = "dev.winter"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

spotless {
    java {
        target("**/*.java")
        targetExclude("build/**")
        googleJavaFormat("1.33.0").aosp().reorderImports(true)
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("build/**")
        ktlint("0.50.0")
    }
}

tasks.register("run") {
    dependsOn(":examples:basic:run")
}
