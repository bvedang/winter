allprojects {
    group = "dev.winter"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

tasks.register("run") {
    dependsOn(":examples:basic:run")
}

