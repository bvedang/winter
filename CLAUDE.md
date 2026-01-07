# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Winter is a minimal Java web framework with file-based routing. The core philosophy is "file path = URL path, method name = HTTP verb."

## Build Commands

```bash
./gradlew run              # Run the example app (examples/basic)
./gradlew build            # Build all modules
./gradlew :core:build   # Build just the core library
```

## Architecture

### Module Structure

- `core/` - The framework library (published as `dev.winter:core`)
- `examples/basic/` - Example application demonstrating usage

### Core Components

**Runtime compilation**: Routes are `.java` files that get compiled at runtime using `javax.tools.JavaCompiler`. The `RouteCompiler` compiles route files on-demand and caches them based on file modification time. Compiled classes are stored in a temp directory (`/tmp/winter-route-cache/`).

**File-based routing**: `FileRouter` maps URL paths to Java files in the routes directory:
- `routes/index.java` → `GET /`
- `routes/users/index.java` → `GET /users`
- `routes/users/[id].java` or `routes/users/[id]/index.java` → `GET /users/:id` (dynamic segment)

### Route File Contract

Each route file must:
- Define `public class Route` (no package statement)
- Export HTTP handlers as `public Object <verb>(Ctx ctx)` where verb is `get`, `post`, `put`, `delete`, or `patch`

Return value handling:
- `String` → `text/plain`
- `Res` → custom status/headers + body
- `null` → 204 No Content
- anything else → JSON via Jackson

### Middleware

Winter supports a small global middleware chain (`winter.middleware.Middleware`) with `before/after/onError` hooks.
See `examples/basic/src/main/java/winter/example/basic/CorsMiddleware.java` for a CORS + OPTIONS preflight example.

### HTTP Behavior

- `HEAD` falls back to `GET` if `head(Ctx)` is not defined
- `OPTIONS` returns `204` with an `Allow` header for matched routes

### Key Classes

- `Ctx` - Request context with `param(name)`, `query(name)`, `queryAll(name)`, `header(name)`, `cookie(name)`, and `body(Class)`
- `Res` - Response builder: `Res.of(status, body).header(name, value)`
- `WinterConfig` - Server config record: routes directory, host, port, max body bytes, expose errors
- `Winter.start(config)` - Boots the Undertow server

## Tech Stack

- Java 21 (toolchain managed)
- Undertow (HTTP server)
- Jackson (JSON serialization)
- Gradle with Kotlin DSL
