# Winter CLI (Design Note)

## Positioning

The Winter CLI is the primary *developer experience* for running Winter apps, but it is **not** a general-purpose Java build tool.

Target audience: Java developers who want a lightweight, Next.js/FastAPI-like workflow **without Spring’s ceremony**, and who still expect IDE/debugger support to work.

## Key Decisions

- **Do not reimplement dependency resolution.** `winter add/remove` should not become “Maven/Gradle in disguise”.
- **Use Gradle under the hood (hidden).** The CLI can generate/maintain an internal Gradle project (temp or hidden in `.winter/`) and use it to:
  - resolve dependencies (battle-tested),
  - compile route code,
  - build runnable artifacts.
- **Keep Winter’s runtime responsibilities in Winter.** Routing, middleware, watcher behavior, and dev-mode error UX stay in Winter’s codebase.

## Known Hard Problems (and how we scope them)

- **Dependency resolution**: leverage Gradle (and later Tooling API) rather than implementing POM parsing, conflict resolution, BOMs, etc.
- **`public class Route` everywhere**: keep the simple authoring model, but compile routes into **unique internal packages** derived from file paths to improve stack traces and class identity (future improvement).
- **Hot reload**: scope “hot reload” to route files; treat shared code/static state/native libs as best-effort and allow restart-on-change outside `routes/`.
- **IDE support**: do not fight the ecosystem; ensure a Gradle project can be imported when desired, even if the CLI hides it by default.

## Phased Implementation Plan

1. **Phase 1 (high leverage): CLI as UX layer over Gradle**
   - `winter new`, `winter dev`, `winter check`, `winter routes`, `winter build`
   - Hide Gradle output; show Winter-native, clean logs/errors.
2. **Phase 2: optimize the happy path**
   - Gradle Tooling API, better caching, faster incremental builds.
3. **Phase 3+: distribution**
   - Optional: bundled runtime (jlink) and native packaging, once UX is stable.

