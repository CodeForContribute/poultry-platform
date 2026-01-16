# Repository Guidelines

## Project Structure & Module Organization

- `backend/`: Spring Boot (Java 21) API, Flyway migrations in `backend/src/main/resources/db/migration/`.
- `seller-web/`: Seller dashboard (Next.js + TypeScript), source in `seller-web/src/`.
- `buyer-mobile/`: Shared/mobile JS workspace (Vitest), source in `buyer-mobile/src/`.
- `buyer-android/`: Android app (Gradle), app code in `buyer-android/app/`.
- `buyer-ios/`: iOS app (Swift Package), code in `buyer-ios/PoultryBuyer/`, tests in `buyer-ios/Tests/`.
- `infra/`: Kubernetes manifests and ops scripts (see `infra/k8s/`).
- `docs/`: Operational and deployment docs.

Generated artifacts (don’t edit/commit): `backend/build/`, `seller-web/.next/`, `**/node_modules/`.

## Build, Test, and Development Commands

- Local stack (recommended): `cp .env.example .env` then `docker-compose up -d` (API on `http://localhost:8080`).
- Backend (local): `cd backend && ./gradlew bootRun` (start), `./gradlew test` (JUnit), `./gradlew build` (build).
- Seller web: `cd seller-web && npm ci`, `npm run dev`, `npm run lint`, `npm test`, `npm run build`.
- Buyer mobile tests: `cd buyer-mobile && npm test`.
- Mobile apps: `cd buyer-android && ./gradlew test`, `cd buyer-ios && swift test`.

## Coding Style & Naming Conventions

- Java: 4-space indentation, keep packages under `com.poultry.*`, prefer small focused services/controllers.
- SQL migrations: follow Flyway naming `V<version>__<description>.sql` and keep them idempotent where possible.
- TypeScript/React: follow existing Next.js patterns; keep imports using `@/…` aliases in `seller-web/`.
- Lint/format: run `seller-web` lint via `npm run lint`; backend quality checks exist in CI (`./gradlew checkstyleMain`, `./gradlew spotbugsMain`).

## Testing Guidelines

- Backend: JUnit 5 + Spring Boot Test + Testcontainers; coverage is enforced via JaCoCo (minimum 50%).
- Frontend: Jest + Testing Library (`seller-web`), Vitest (`buyer-mobile`).
- Naming: `*Test.java` for unit tests; prefer `*IntegrationTest.java` for container-backed flows.

## Commit & Pull Request Guidelines

- Commits: repo history is free-form; use short imperative subjects and add a module prefix when helpful (e.g., `backend: add settlement reconciliation`).
- PRs: include a clear description, linked issue(s), test evidence (commands run), and screenshots for UI changes (`seller-web/`). Avoid committing secrets; use `.env.example`/`.env.production.example` as templates.
