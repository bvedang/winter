# Using Winter Locally (Today)

Winter is early-stage and not published to Maven Central yet. The simplest way to use it **right now** is to build your app inside this repo (or start from the included example).

## Requirements

- Java **21 JDK** (not just a JRE). Winter compiles route files at runtime via `javax.tools.JavaCompiler`.
- Use the Gradle wrapper (`./gradlew`).

## Option A (Fastest): Use the Included Example App

Run:

```bash
./gradlew run
```

Edit routes in `examples/basic/routes/` and refresh the browser.

## Option B (Recommended for a Real App): Add an `apps/` Module Here

This keeps everything local and avoids publishing/versioning while Winter evolves.

1) Create an app module:

- `apps/my-api/routes/index.java`
- `apps/my-api/src/main/java/my/api/Main.java`

2) Add it to `settings.gradle.kts`:

```kotlin
include(":apps:my-api")
project(":apps:my-api").projectDir = file("apps/my-api")
```

3) Add `apps/my-api/build.gradle.kts`:

```kotlin
plugins { application }
dependencies { implementation(project(":core")) }
application { mainClass.set("my.api.Main") }
```

4) Minimal `Main.java`:

```java
package my.api;

import java.nio.file.Path;
import winter.Winter;
import winter.WinterConfig;

public final class Main {
  public static void main(String[] args) {
    var config = WinterConfig.of(Path.of("routes"))
      .withHost("127.0.0.1")
      .withPort(8080)
      .withHotReload(true);
    Winter.start(config);
  }
}
```

5) Run:

```bash
./gradlew :apps:my-api:run
```

Note: `Path.of("routes")` is resolved relative to the process working directory. If your run task doesn’t set it, point to an absolute path (or update the Gradle `run` task to set `workingDir`).
