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

- **Hot reload**: `WatchService` + cache invalidation + recompilation behavior
- **Route tree caching**: avoid directory scans per request in dev; keep correctness first
- **Graceful shutdown**: drain in-flight requests, configurable timeout
- **Middleware ergonomics**: route-scoped middleware and ordering helpers
- **DX polish**: clearer error messages for ambiguous routes, route compilation diagnostics UX
- **Docs**: “How to build an app with Winter” guide
