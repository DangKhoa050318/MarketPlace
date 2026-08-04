# Marketplace Repository Conventions

These instructions apply to the entire repository.

## Project context

- This is a single-seller marketplace implemented as a modular monolith.
- `backend/` is Spring Boot 3.2 on Java 17. The base package is
  `com.training.marketplace`.
- `frontend/` is Angular 17 with standalone components.
- Read `README.md` for local setup and `MERGE-STATUS.md` for current implementation
  status before starting substantial work. Treat newer progress entries in
  `MERGE-STATUS.md` as authoritative when older checklist sections conflict.

## Architecture invariants

- The catalog is two-tiered: `Product` is the SPU and `ProductVariant` is the
  sellable SKU.
- Cart, order, pricing, and inventory operations must use `variantId`, not
  `productId`, when identifying a sellable item.
- Inventory truth lives in `stock_levels`, keyed by
  `(variant_id, warehouse_id)`.
- All stock reservation, fulfillment, and release operations must go through
  `InventoryFacade`. Preserve its locking behavior and do not update stock
  ad hoc from controllers or unrelated services.
- Preserve the roles `ADMIN`, `MANAGER`, `STAFF`, and `CUSTOMER`; do not
  reintroduce the removed `USER` role.
- Keep controllers thin. Put business rules in services and persistence logic
  in repositories.

## Backend conventions

- Follow the existing package layout and naming: controllers, DTOs, mappers,
  services/interfaces, `service.impl`, repositories, entities, consumers, and
  publishers.
- Use request/response DTOs at API boundaries; do not expose JPA entities.
- Return the existing `ApiResponse<T>` and `PageResponse<T>` envelopes where
  surrounding endpoints use them.
- Use Jakarta validation annotations on request DTOs and the existing global
  exception-handling approach.
- Use constructor injection. Follow the existing Lombok and MapStruct patterns
  rather than adding manual boilerplate without a reason.
- Maintain transaction boundaries in the service layer. For inventory changes,
  retain deterministic lock ordering to avoid overselling and deadlocks.
- API routes remain under `/api/v1`. Update OpenAPI annotations/schemas when an
  endpoint contract changes.
- **Flyway migrations use timestamp versioning, not sequential numbers.** Name
  every new migration `V<yyyyMMddHHmmss>__<description>.sql` using the moment you
  create it — e.g. `V20260729143000__add_campaign_tables.sql`. This stops version
  collisions between parallel feature branches (the old `V1`, `V2`, … scheme
  repeatedly clashed on merge). Do **not** introduce sequential `Vn` versions.
- Never edit an applied Flyway migration. Add a new timestamped migration and
  keep entity mappings compatible with `ddl-auto: validate`. Migrations must
  apply cleanly on a fresh database (`docker compose down -v`); order them by the
  data dependencies they need, not by authoring time.
- The `backend/scripts/check-migration-versions.sh` guard (run in CI) fails the
  build on duplicate versions or any old-style `Vn` migration.
- Keep secrets and machine-specific values out of source control. Add documented
  placeholders to `.env.example` when introducing configuration.

## Frontend conventions

- Use standalone Angular components and the existing `core/`, `shared/`,
  `layout/`, and `features/` organization.
- Keep HTTP access in typed services under `core/services` (or the owning
  feature when already established), not directly in components.
- Reuse the shared API/page models and keep frontend interfaces synchronized
  with backend DTOs.
- Use Angular reactive patterns and `Observable` return types. Avoid nested
  subscriptions; compose streams with RxJS operators.
- Use Angular Material and existing shared components before introducing a new
  UI dependency.
- Use environment configuration for API base URLs; do not hard-code backend
  hosts in components or services.
- Preserve the repository's TypeScript formatting: two-space indentation,
  single quotes, and trailing semicolons.

## Testing and verification

- Add or update focused tests with every behavior change. Prefer tests at the
  service layer for business rules and controller tests for HTTP contracts.
- Unit tests must not require Docker. Integration tests may use the existing
  Testcontainers setup.
- Run the narrowest relevant tests during iteration, then the applicable suite:

```powershell
cd backend
.\mvnw.cmd test -Dtest=RelevantTest
.\mvnw.cmd test "-Dtest=!*IntegrationTest"

cd ..\frontend
npm test -- --watch=false
npm run build
```

- Run backend integration tests only when Docker and the required services are
  available:

```powershell
cd backend
docker compose up -d
.\mvnw.cmd verify
```

- Do not claim a fix is complete without reporting which checks ran and their
  results. If a check could not run, state the concrete reason.

## Change discipline

- Keep changes scoped to the requested task; do not rewrite unrelated code.
- Preserve unrelated user changes in the working tree.
- Search for all consumers before renaming DTO fields, routes, events, database
  columns, or model identifiers.
- When changing an API contract, update backend DTOs/controllers, frontend
  models/services, tests, OpenAPI documentation, and Postman assets as
  applicable.
- Update `README.md` or `MERGE-STATUS.md` when setup, architecture, verified
  status, or known follow-up work materially changes.
