# Winter Status

## Implemented (Working End-to-End)

- **Gradle multi-module**: `core/` (framework) + `examples/basic/` (demo app)
- **HTTP server**: Undertow boot via `Winter.start(...)`
- **File-based routing**
  - Static: `routes/index.java`, `routes/users/index.java`, `routes/users/show.java` (leaf file)
  - Dynamic file: `routes/users/[id].java`
  - Dynamic directory + nesting: `routes/users/[id]/index.java`, `routes/users/[id]/posts/index.java`, `routes/[category]/[id].java`
  - **Precedence**: static dir > static file (leaf) > dynamic dir > dynamic file (leaf)
  - **Ambiguity errors**: multiple dynamic dirs/files at the same level is a configuration error
  - **Path traversal hardened**: segment validation + `routesDir` containment enforcement
- **Runtime compilation**: `.java` routes compiled on-demand with `JavaCompiler`, cached by mtime, old classloaders closed
- **Hot reload (routes)**: `WatchService` precompiles routes at boot and recompiles on change (`WinterConfig.withHotReload(true)`)
- **Ctx**
  - `param`, `query`, `queryAll`
  - `header`, `headers(name)`, `headers()` map
  - `cookie`, `cookies()`
  - `bodyBytes`, `bodyText`, `body(Class)` (JSON parse returns 400)
  - Request body size cap (413 on overflow)
- **Responses**
  - Return `String` → text, otherwise JSON
  - Return `Res` → status/headers/body
  - `HttpError(status, body)` → returns that status/body
  - 500 error leakage disabled by default (`WinterConfig.exposeErrors=false`)
- **Middleware (global)**
  - `before/after/onError` pipeline
  - Example CORS middleware + OPTIONS preflight in `examples/basic`
- **HTTP behavior**
  - `HEAD` falls back to `GET` when no `head(Ctx)` is defined
  - `OPTIONS` returns 204 + `Allow` (for matched routes)
  - 405 includes `Allow`
- **Smoke testing**
  - `scripts/smoke.sh` exercises routing, nested dynamics, headers/cookies/queryAll, CORS, HEAD/OPTIONS, 405 Allow, traversal guard, and `HttpError`

## Remaining (Next Pragmatic Milestones)

- **Route tree caching**: avoid directory scans per request in dev; keep correctness first
- **Graceful shutdown**: drain in-flight requests, configurable timeout
- **Middleware ergonomics**: route-scoped middleware and ordering helpers
- **DX polish**: clearer error messages for ambiguous routes, route compilation diagnostics UX
- **Docs**: “How to build an app with Winter” guide
- **CLI (design)**: see `docs/CLI.md`

## DX Backlog (Ideas)

These are product/DX ideas (inspired by Next.js App Router + FastAPI). They are tracked here for prioritization; not all belong in `core/`.

### Zero-Friction Start

- [ ] `create-winter-app` or a Gradle init plugin to scaffold a new app:
  - `routes/index.java`
  - `build.gradle.kts`, `settings.gradle.kts`, `.gitignore`
  - `./gradlew dev`

### Dev Mode with Browser Errors

- [ ] `WinterConfig.withDevMode(true)` to render compilation/runtime errors in the browser (HTML overlay) while still logging to terminal.

### Request Validation (FastAPI-style)

- [ ] Validation annotations for request bodies (e.g. `@Email`, `@Min`) and structured 400 responses:
  - `ctx.body(Type.class)` validates automatically
  - returns `{ "error": "Validation Failed", "details": [...] }`

### Auto-Generated API Docs

- [ ] OpenAPI generation + Swagger UI at `/docs` (likely optional module).

### Path & Query Param Type Coercion

- [ ] `ctx.param("id", Integer.class)` and `ctx.query("page", Integer.class, 1)` with 400 on invalid inputs.

### Response Helpers

- [ ] Convenience helpers: `Res.ok(...)`, `Res.created(...)`, `Res.noContent()`, `Res.badRequest(...)`, etc.

### Built-in Middleware Library

- [ ] Common middleware helpers (likely optional module/package):
  - CORS, request-id, logger, rate-limit, basic-auth

### Static Files & Assets

- [ ] Static file mapping:
  - `WinterConfig.withStatic("/public", Path.of("static"))`

### Environment & Config

- [ ] `.env` support and typed env helpers:
  - `Winter.env("PORT", 8080)`
  - `Winter.env("DATABASE_URL")` (required)

### Better Error Messages

- [ ] Dev-mode JSON error payloads with location/snippet/hints (while keeping production output minimal).

### Testing Utilities

- [ ] A `WinterTest` base class / helpers for route tests:
  - `get("/users")`, `post("/users", body)`
  - `assertStatus(201)`, `assertJson("$.id", ...)`, `assertHeader(...)`

### CLI Tool

- [ ] `winter` CLI:
  - `winter new`, `winter dev`, `winter build`, `winter routes`, `winter generate route ...`

### Watch Mode with True Hot Reload

- [ ] Watcher that recompiles routes instantly + restarts on code/config changes, with clear terminal feedback.

### First-Class JSON Support

- [ ] Expose JSON configuration:
  - `WinterConfig.withJson(json -> ...)`

### Priorities (Impact vs Effort)

| Priority | Feature                                    | Impact    | Effort |
|----------|--------------------------------------------|-----------|--------|
| 1        | Type coercion                              | High      | Low    |
| 2        | Response helpers                           | High      | Low    |
| 3        | Dev mode browser errors                    | High      | Medium |
| 4        | Built-in CORS/logging middleware           | High      | Low    |
| 5        | Validation annotations                     | Very High | Medium |
| 6        | CLI + project scaffold                     | High      | Medium |
| 7        | Static file serving                        | Medium    | Low    |
| 8        | .env loading                               | Medium    | Low    |
| 9        | Test utilities                             | Medium    | Medium |
| 10       | Auto API docs                              | High      | High   |

### “5-Minute Demo” Bar

- [ ] Create project in ~30s (`winter new my-api`)
- [ ] Run dev server in ~10s (`winter dev`)
- [ ] Edit `routes/index.java` and see changes instantly
