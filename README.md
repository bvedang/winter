# Winter

Minimal Java web framework. Anti-Spring.

File path = URL path. Method name = HTTP verb.

## Setup

```bash
# Java 21 required
brew install openjdk@21

# Enable git hooks (formatting + conventional commits)
git config core.hooksPath .githooks

# Verify
./gradlew build
```

**Optional** - For format-on-save in Zed, install `google-java-format`:

```bash
brew install google-java-format
```

Then add to Zed/VScode settings:

```json
{
    "languages": {
        "Java": {
            "formatter": {
                "external": {
                    "command": "google-java-format",
                    "arguments": ["--aosp", "-"]
                }
            }
        }
    }
}
```

## Dev

Run the example app:

```bash
./gradlew run
```

Example routes live in `examples/basic/routes/`.

Smoke test:

```bash
./scripts/smoke.sh
```

Dev helpers:

- `./scripts/watch.sh` (requires `watchexec` or `entr`)
- `./scripts/clean-cache.sh` (clears the route compiler cache)
- `./scripts/new-route.sh /users/[id]` (scaffolds a new route)

Current status/backlog: `docs/STATUS.md`.

## Routing Contract (MVP)

- `routes/index.java` → `GET /`
- `routes/users/index.java` → `GET /users`
- `routes/users/[id].java` or `routes/users/[id]/index.java` → `GET /users/:id` via `ctx.param("id")`
- Each file defines `public class Route`
- Each handler is `public Object get(Ctx ctx)` / `post` / `put` / `delete` / `patch`
- Return value:
    - `String` → `text/plain`
    - `Res` → status/headers + body
    - anything else → JSON

`Ctx` helpers:

- `ctx.param(name)`, `ctx.query(name)`, `ctx.queryAll(name)`
- `ctx.header(name)`, `ctx.headers(name)`
- `ctx.cookie(name)`

## Middleware (MVP)

Register global middleware when starting:

```java
Winter.start(config, new MyMiddleware());
```

Middleware hooks (`winter.middleware.Middleware`):

- `before(ctx)` can short-circuit by returning a response object (often `Res`)
- `after(ctx, result)` can wrap/transform the response (e.g. add headers)
- `onError(ctx, exception)` can handle exceptions and return a response

## HTTP (MVP)

- `HEAD` falls back to `GET` (same status/headers, no body)
- `OPTIONS` returns `204` and includes an `Allow` header for matched routes

## Config (MVP)

- `WinterConfig.DEFAULT_MAX_BODY_BYTES` is enforced for request bodies (413 on overflow)
- Set `WinterConfig.withExposeErrors(true)` to include exception messages in 500s (default: false)
- Set `WinterConfig.withHotReload(true)` to watch `routes/` and recompile on change (default: false)
