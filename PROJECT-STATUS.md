# Marketplace — Project Status

Single source of truth for **current project progress** (replaces the old `MERGE-STATUS.md`, whose job —
tracking the one-time merge of the two legacy apps into MarketPlace — is finished). Update this file when a
feature/workstream materially changes state.

Legend: ✅ done · 🟡 partial · ⏳ not started · owners per the 3-Week roadmap
(`StockPulse-Ecommerce-Features-3-Week-Requirements.md`).

## Foundation
- ✅ Merge of the two legacy apps (storefront + warehouse) into one modular monolith (Spring Boot 3.2 / Java 17 / Angular 17); compiles & boots.
- ✅ 2-tier catalog: `products` (SPU) → `product_variants` (SKU); stock truth in `stock_levels(variant_id, warehouse_id)`; order→stock via `InventoryFacade` (pessimistic lock, no oversell).
- ✅ Flyway **timestamp** migration convention (`V<yyyyMMddHHmmss>__desc.sql`) + CI guard (`backend/scripts/check-migration-versions.sh`, `.github/workflows/backend-ci.yml`).
- Infra: PostgreSQL 5433, Redis, RabbitMQ (order events + DLQ), MailHog. Event-driven order flow.

## Feature workstreams

| Feature | Owner | Status | Notes |
|---|---|---|---|
| **STP-01** Reviews, Q&A & Moderation | GiangHV9 | ✅ done (on dev) | Reviews/ratings, Q&A, helpful votes, moderation workflow + audit log |
| **STP-02** Promotions & Merchandising | KhoaNXD1 | 🟡 | **Wk1 Coupon Engine** ✅ (on dev). **Wk2 Campaigns/Collections/Merchandising** backend ✅ + admin UI F-401→403 ✅ (branch `feature/stp-02-campaigns-collections-merchandising`); **F-404→406 + tests T-401→406 ⏳**; PO decisions D-1/D-5 pending (`docs/feature-stp-02-po-decisions.md`) |
| **STP-03** Personalized Recommendations | HoangNQ17 | ✅ done (on dev) | Similar / co-viewed / co-purchased, recently-viewed, wishlist. Ref docs: `docs/recommendation-*.md`, `docs/*-recommendation.md`, `docs/analytics-event-schema-v1.md` |
| **STP-04** CX / Analytics | TriTVV2 | ✅ done (on dev) | Journey analytics + funnel, anonymous journey merge/retention, analytics overview KPIs (B-801), product performance (B-802), analytics export |
| **STP-05** Bundle | Giang + Khoa (Wk3) | ⏳ | not started |
| **STP-06** Comparison | Hoang + Tri (Wk3) | ⏳ | not started; needs brand/EAV attributes (out of current catalog scope) |

## Base-code hardening (from the main-branch security review)

Branch `fix/base-security`.

| # | Item | Status |
|---|---|---|
| A 🔴 | JWT: no hardcoded default; `JWT_SECRET` required (fail-fast) | ✅ fixed |
| F 🟡 | RabbitMQ converter registers `JavaTimeModule` | ✅ fixed |
| J 🟢 | Rate-limit keyed by username when authenticated | ✅ fixed |
| L 🟢 | Tighter `/auth/login` cap (5/min) vs brute-force | ✅ fixed |
| K 🟢 | README warns seed passwords/JWT are local-only | ✅ fixed |
| B 🔴 | Refresh token revocable (Redis store + rotation + logout) | ✅ fixed |
| D 🟠 | Payment consumer: split transactions + idempotent compensation | ✅ fixed |
| G 🟡 | Retire `MERGE-STATUS.md` → this file | ✅ fixed |
| E 🟠 | Oversell blocked at order time (reserve before create) | ✅ already correct — add a test |
| C 🟠 | Real payment gateway (currently mocked, always succeeds) | ⏳ team decision |
| H 🟡 | End-to-end payment-reconciliation test | ⏳ depends on C |

**⚠️ Operational:** `JWT_SECRET` is now required to run the backend (`export JWT_SECRET="$(openssl rand -base64 48)"`). Existing refresh tokens are invalidated after this deploy (re-login needed).

## Known gaps / follow-ups
- STP-02 Wk2 storefront rendering (F-404), analytics hooks (F-405), effectiveness table (F-406) + tests T-401→406.
- Integration tests T-304/T-306 and STP-02-Wk2 tests run in **CI** (Testcontainers can't reach Docker Desktop on Windows locally).
- `/actuator/health` historically 500 (MailHog health indicator) — verify.
- STP-06 comparison needs a brand/attribute (EAV) model the catalog currently lacks.

## Doc map (reference docs to keep)
- `README.md` (setup) · `AGENTS.md` (conventions) · `StockPulse-Ecommerce-Features-3-Week-Requirements.md` (feature requirements, source of truth).
- STP-03: `docs/recommendation-api.md`, `docs/similar|best-seller|co-occurrence-recommendation.md`, `docs/recommendation-eligibility-filter.md`, `docs/analytics-event-schema-v1.md`.
- STP-02: `docs/feature-stp-02-week2-plan.md` (active plan), `docs/feature-stp-02-po-decisions.md` (awaiting PO), `docs/feature-stp-02-demo.md` + `docs/demo-full-project.md` (demo scripts).
