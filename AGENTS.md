# Repository Guidelines

## Project Structure & Module Organization

- `core/`: Winter framework library (Undertow server, router, compiler, middleware).
  - Main code: `core/src/main/java/winter/`
  - Tests: `core/src/test/java/` (unit + integration)
- `examples/basic/`: runnable demo app with file-based routes.
  - Example routes: `examples/basic/routes/`
- Docs: `README.md`, `docs/STATUS.md`
- Scripts: `scripts/smoke.sh` (end-to-end sanity check)

## Build, Test, and Development Commands

- `./gradlew build`: builds all modules.
- `./gradlew run`: runs the example app (`examples/basic`) on `http://127.0.0.1:8080`.
- `./gradlew :core:test`: runs unit + integration tests for the framework.
- `./scripts/smoke.sh`: boots the example server and curls key endpoints (routing, CORS, HEAD/OPTIONS, safety checks).

## Coding Style & Naming Conventions

- Java 21+; prefer modern Java (`var` where it improves readability).
- Indentation: 4 spaces; keep classes small and focused.
- Public API names stay explicit (`Winter`, `WinterConfig`, `WinterServer`, `Ctx`, `Res`).
- Routes follow file-based routing (Next.js-style):
  - `routes/index.java` → `/`
  - `routes/users/[id]/index.java` → `/users/:id`
  - Dynamic segments use `[param]` in directory/file names.

## Testing Guidelines

- Framework tests use **JUnit 5**.
- Prefer behavior-driven tests over coverage chasing:
  - Unit tests: routing resolution and edge cases (`FileRouterTest`).
  - Integration tests: start Undertow and assert real HTTP behavior (`WinterIntegrationTest`).

## Commit & Pull Request Guidelines

- No strong commit-message convention is established yet; use clear, imperative messages (or Conventional Commits if you prefer).
- PRs should include:
  - a short “what/why” summary,
  - how to verify (`./gradlew :core:test`, `./scripts/smoke.sh`),
  - notes on any API/behavior changes.

## Security & Configuration Notes

- Route resolution is hardened against traversal; keep it that way when changing routing.
- Request bodies are size-limited via `WinterConfig.maxBodyBytes`; `exposeErrors` is `false` by default.
