plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "winter"

include(":core")
include(":examples:basic")

project(":examples:basic").projectDir = file("examples/basic")
