plugins {
    id("com.diffplug.spotless") version "6.25.0"
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
        googleJavaFormat("1.21.0").aosp()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("**/*.kts")
        targetExclude("build/**")
        ktlint()
    }
}

tasks.register("run") {
    dependsOn(":examples:basic:run")
}
