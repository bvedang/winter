plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "winter"

include(":core")
include(":examples:basic")

project(":examples:basic").projectDir = file("examples/basic")
