# Marketplace — Merge Status & Continuation Guide

Tracks the merge of **OrderFlow** (storefront) + **StockPulse** (warehouse) into one modular
monolith, per `docs/Project - Marketplace (Merged Spec).md` (v0.2: monolith · single-seller ·
2-tier catalog with `product_variants`).

> **Trạng thái tổng (2026-08-03): Stage 1–3 đã hoàn tất; backend compile/test-compile và frontend
> build đều xanh.** Catalog, cart, order và inventory đã dùng `variantId`; tồn kho đi qua
> `InventoryFacade`. Các checklist Stage 2 bên dưới đã được cập nhật theo code hiện tại.

---

## 📋 Feature workstreams — tổng quan toàn dự án (cập nhật 2026-08-03)

> File này giờ là **tracker tiến độ toàn dự án** (không chỉ merge 2 app cũ). Đã gộp nội dung
> `PROJECT-STATUS.md` vào đây — file đó đã bỏ. Chi tiết từng mục xem các log theo ngày bên dưới.

Legend: ✅ done · 🟡 partial · ⏳ chưa bắt đầu.

| Feature | Owner | Trạng thái | Ở đâu / ghi chú |
|---|---|---|---|
| Nền — merge OrderFlow + StockPulse (Stage 1–3) | cả nhóm | ✅ done | `dev` — compile/boot, catalog 2 tầng, `InventoryFacade` chống oversell |
| **STP-01** Reviews, Q&A & Moderation | GiangHV9 | ✅ done | `dev` — reviews/ratings, Q&A, helpful vote, moderation + audit log |
| **STP-02** Promotions & Merchandising | KhoaNXD1 | 🟡 gần xong | **Wk1 Coupon ✅ `dev`**. **Wk2 Merchandising ✅ trên nhánh `feature/stp-02-campaigns-collections-merchandising`** (đã merge `dev`, build xanh): backend + admin UI **F-401→403** + storefront **F-404/405/406** + unit tests. Còn: integration/E2E **T-402/403/406** (Docker/Playwright-gated) + PO **D-1/D-5** (`docs/feature-stp-02-po-decisions.md`). **Sẵn sàng PR về `dev`** |
| **STP-03** Personalized Recommendations | HoangNQ17 | ✅ done | `dev` — similar / co-viewed / co-purchased / best-seller, eligibility filter, storefront carousel (B-501→508, F-501→506, T-501→506) |
| **STP-04** Analytics / CX / Journey | TriTVV2 | ✅ done | `dev` — `AdminAnalyticsController`, export, funnel/KPI, `JourneyMergeController` (gộp hành trình ẩn danh ↔ đăng nhập) |
| **STP-05** Bundle | Giang + Khoa (Wk3) | ⏳ | chưa bắt đầu |
| **STP-06** Comparison | Hoang + Tri (Wk3) | ⏳ | chưa bắt đầu; cần model brand/attribute (EAV) catalog hiện chưa có |
| Feature-03 Merchant working-capital loan (liên kết GatePay) | KhoaNXD1 | ⏳ blocked | GatePay (Trí) chưa giao API `/merchant-loans/*`; MarketPlace là client/proxy. Plan: `docs/feature-03-working-capital-marketplace.md` |
| PR #37 — shipping fee + order variant UI + image upload + review-per-order-item | — | ✅ done | `dev` (merged 2026-08-03): phí ship theo đơn, upload ảnh review, review theo từng order item |

**Base-code hardening (main-branch security review):** A/F/J/K/L (PR #36) + B/C/D/E/H (c90309b) + G — chi tiết ở log **"Review hardening follow-up — 2026-08-03"** bên dưới.
⚠️ Vận hành: `JWT_SECRET` **bắt buộc** set (app fail-fast nếu thiếu); `PAYMENT_PROVIDER=disabled` (fail-closed — đơn giữ PENDING tới khi cắm provider thật).

**Doc map (giữ lại — đã kiểm tra 2026-08-03, chưa doc nào tới lúc xoá):**
- `README.md` (setup) · `AGENTS.md` (convention) · `docs/StockPulse-Ecommerce-Features-3-Week-Requirements.md` (**source of truth**) · `docs/Project - Marketplace (Merged Spec).md` (đặc tả nền).
- STP-03 reference: `docs/recommendation-api.md`, `docs/similar|best-seller|co-occurrence-recommendation.md`, `docs/recommendation-eligibility-filter.md`, `docs/recommendation-demo-data.md`, `docs/analytics-event-schema-v1.md`.
- STP-02: `docs/feature-stp-02-week2-plan.md` (plan — **lưu ý D-4 nói reuse `analytics_events` nhưng code thực tế dùng bảng riêng `merchandising_events`**), `docs/feature-stp-02-po-decisions.md` (chờ PO), `docs/feature-stp-02-demo.md`.
- Demo & reference: `docs/demo-full-project.md` (kịch bản demo toàn dự án), `docs/FEATURES-AND-API-FLOWS.md` (mô tả API theo code — mục "12 file null-byte" đã cũ, các file đó đã khôi phục).

---

## ✅ Stage 1 — ĐÃ XONG (physical merge + foundation)

- [x] Tạo folder `Marketplace/` (base = OrderFlow); copy `backend/` + `frontend/`.
- [x] Overlay các file **inventory** của StockPulse (warehouse/stock/movement/alert/reorder,
      messaging, projections, stock DTOs/mappers/repos/services) — 54 file, không đụng tên với base.
- [x] Đổi base package `com.training.starter` → `com.training.marketplace` (toàn bộ src + move thư mục).
- [x] Đổi main class `StarterApplication` → `MarketplaceApplication`.
- [x] `pom.xml`: artifactId/name → `marketplace`.
- [x] `application.yml`: DB `marketplace_db` @ port **5433**; logging package `com.training.marketplace`.
- [x] `.env.example`: DB_NAME/DB_PORT.
- [x] `docker-compose.yml`: containers `marketplace-*`, postgres `5433:5432`.
- [x] **Flyway V1–V8 viết lại sạch** (schema gộp, khóa theo `variant_id`):
      V1 users(+seed) · V2 categories+products+**product_variants**(+seed) · V3 warehouses+stock_levels ·
      V4 orders+order_items · V5 stock_movements+items · V6 alerts+reorder_suggestions · V7 indexes ·
      V8 mv_stock_summary + search trigger.
- [x] **Entities** cập nhật theo schema: `Role{ADMIN,MANAGER,STAFF,CUSTOMER}`, `Category`(code+slug+parentId),
      `Product`(SPU, Long categoryId), **`ProductVariant`(mới, SKU)**, `StockLevel`/`StockMovementItem`/
      `OrderItem`/`ReorderSuggestion` → `variantId`, `Order` +`warehouseId`.
- [x] Sửa `Role.USER` (enum đã bỏ) → `Role.CUSTOMER` ở 4 file (User, Auth, Order, User service).

### Deviations so với spec (có lý do)
- **Thứ tự Flyway orders↔movements đảo:** V4=orders, V5=movements (spec để movements V4). Vì
  `stock_movements.source_order_id` tham chiếu `orders(id)` nên orders phải tạo trước.
- **`Order`/`OrderItem` giữ `@ManyToOne`** (theo style OrderFlow) thay vì Long FK — chỉ áp dụng cho
  cụm ordering; các entity khác theo rule Long FK.

---

## 🔧 Stage 2 — reconcile backend cho compile + xanh test

Mô hình variant đã đổi ở entity/schema; giờ sửa các file còn tham chiếu model cũ. **Danh sách chính
xác (từ grep):**

### ✅ Progress log — cập nhật 2026-07-26: **MAIN BACKEND COMPILE XANH** (`./mvnw clean compile` = BUILD SUCCESS, 144 file)
**Đã reconcile toàn bộ src/main sang mô hình variant:**
- ✅ Copy `config/OpenApiSchemas.java`; `Role.USER`→`Role.CUSTOMER`.
- ✅ **Catalog vertical (SPU/SKU):** DTOs, `ProductMapper`+`ProductVariantMapper`, `ProductRepository`
     (bỏ product-lock)+`ProductVariantRepository`, `ProductService`/`Impl`+`ProductVariantService`/`Impl`,
     `ProductController`+`ProductVariantController`.
- ✅ **Inventory:** `StockLevel*`/`Movement*` responses+mappers→variant; `StockLevelRepository`,
     `StockSummaryRepository`, `ReorderSuggestionRepository`, projections → `@Query`/getter theo `variant_id`;
     `StockLevelServiceImpl` + `StockMovementServiceImpl` (`complete()` khóa & cập nhật stock_levels theo variant).
- ✅ **Ordering + tích hợp:** `CartServiceImpl`(+cart DTOs/controller theo variantId), **`InventoryFacade`
     (+Impl) reserve/fulfill/release** — điểm khóa no-oversell; `OrderServiceImpl` đặt đơn = reserve, huỷ = release,
     SHIPPED = fulfill (trừ tồn thật); `OrderItem`/`OrderItemResponse`/`OrderMapper`/`OrderCreatedEvent` theo variant.
- ✅ **Consumers:** `PaymentConsumer` (thất bại → release qua facade), `StockUpdateConsumer`/`ReorderConsumer`
     + `StockMovementCompletedEvent`/`StockLowEvent` → `variantId(s)`.
- ✅ **Security RBAC:** `SecurityConfig` cập nhật roles `{CUSTOMER,STAFF,MANAGER,ADMIN}` + matcher warehouse.
- ✅ V7 thêm partial-unique index cho `reorder_suggestions(variant_id,warehouse_id) WHERE status='PENDING'`.

### ✅ Stage 2b — XONG (test-compile xanh + 62 unit test PASS)
- [x] **Category `code`+`parentId`** hoàn tất: `CreateCategoryRequest`(name/code/slug/parentId),
      `UpdateCategoryRequest`(+parentId), `CategoryResponse`, `CategoryMapper` (code immutable),
      `CategoryServiceImpl` (existsByCode + `validateParent` chống cycle), `CategoryRepository.existsByCode`.
- [x] **19 file test compile xanh** (`./mvnw test-compile` = BUILD SUCCESS). Đã viết lại fixtures theo variant:
      OrderServiceTest (reserve qua InventoryFacade), ProductServiceTest (SPU+variants), CartServiceTest (variantId),
      CategoryServiceTest (code/parentId), Product/Category/CartControllerTest, consumer/publisher tests
      (OrderItemInfo +sku, OrderCreatedEvent +warehouseId), Role.USER→CUSTOMER, RBAC roles CUSTOMER.
- [x] **ConcurrentStockLockIntegrationTest viết lại** = test đồng thời chống oversell trên **1 variant** qua
      `InventoryFacade.reserve` (10 luồng, tồn=10 → 10 thành công, available=0). *(cần Docker để CHẠY)*
- [x] **62 unit test PASS** (`./mvnw -o test -Dtest='!*IntegrationTest'` = BUILD SUCCESS).

### ✅ App KHỞI ĐỘNG ĐƯỢC (đã verify chạy thật với Docker — 2026-07-27)
`docker compose up -d` + `./mvnw spring-boot:run` → **Started MarketplaceApplication in ~8.6s**.
- Flyway **V1–V8 áp dụng sạch**; Hibernate `ddl-auto: validate` **PASS** (entity khớp schema).
- Swagger `/swagger-ui/index.html` → HTTP 200; `GET /api/v1/products` trả seed; `GET /api/v1/products/1`
  trả SPU **kèm variants** (sku/price/color/size) — mô hình 2 tầng chạy end-to-end.
- **2 bug wiring phát hiện & sửa khi boot thật:**
  1. Thiếu bean `RedisTemplate<String, StockLevelResponse>` (bản StockPulse có, OrderFlow không) →
     thêm `stockRedisTemplate` vào `RedisConfig`.
  2. Thiếu khai báo queue/exchange stock (StockPulse để trong `RabbitMQConfig` mình không copy;
     `StockRabbitTopology` chỉ là hằng số) → thêm `config/StockRabbitConfig` (stock.exchange + 4 queue + binding).
- **Lưu ý nhỏ:** `/actuator/health` trả 500 (một health indicator — nhiều khả năng `mail`/MailHog — ném lỗi);
  không ảnh hưởng API nghiệp vụ. Có thể tắt bằng `management.health.mail.enabled=false` nếu cần.

### 🔧 Còn lại (không chặn build/boot)
- [ ] Các integration test (`*IntegrationTest`, Testcontainers) **compile xanh**; chạy bằng
      `./mvnw verify` khi Docker khả dụng. Đã thêm `PaymentReconciliationIntegrationTest` cho
      success/failure, Rabbit retry và đối soát reservation.
- [ ] *(tuỳ chọn)* Cầu async `order.confirmed → stock.export.queue`: hiện làm **đồng bộ** trong
      `OrderServiceImpl` (reserve khi đặt, fulfill khi SHIPPED) — đúng & chống oversell tốt hơn.
- [x] `jsonMessageConverter` đã đăng ký `JavaTimeModule` và xuất ngày giờ dạng ISO-8601.
- [ ] Payment provider thật chưa được chọn. `PaymentGateway`/ledger/reconciliation đã có và cấu hình
      mặc định `disabled` để fail closed; cần adapter Stripe/VNPay/MoMo sau khi team chốt provider và
      checkout contract tương ứng.

Lệnh: `./mvnw -o clean compile` = **BUILD SUCCESS** · `./mvnw -o test-compile` = **BUILD SUCCESS** ·
`./mvnw -o test -Dtest='!*IntegrationTest'` = **62 pass**.

### 2a. Catalog (SPU/variant) — ✅ XONG
- [x] DTOs Product (SPU) + Variant (Create/Update/Response).
- [x] `ProductMapper` + `ProductVariantMapper`.
- [x] `ProductService`/`Impl` + `ProductVariantService`/`Impl` + `ProductVariantRepository`.
- [x] `ProductController` + `ProductVariantController`; `ProductRepository` (bỏ product-lock).
- [x] `Category*`: DTO/mapper/service đã hỗ trợ `code`+`parentId`, gồm kiểm tra parent và cycle.

### 2b. Ordering (khóa theo variant + tồn từ stock_levels) — bắt buộc
- [x] `service/impl/OrderServiceImpl`: bỏ `product.getStock()/getPrice()`; giá lấy từ **variant**,
      tồn/khóa lấy từ **stock_levels** qua `InventoryFacade` (reserve/fulfill — §7 spec). Snapshot
      `sku/variantName` vào order_items.
- [x] `service/impl/CartServiceImpl` + `CartItemResponse` + `AddToCartRequest`/`UpdateCartItemRequest`:
      cart key theo **variantId**, giá từ variant.
- [x] `mapper/OrderMapper`, `dto/.../CreateOrderRequest`: theo variant.
- [x] `consumer/PaymentConsumer`: event/order item dùng variant; xử lý qua payment ledger idempotent.

### 2c. Inventory (StockPulse-origin → variant) — bắt buộc
- [x] `service/impl/StockLevelServiceImpl`, `StockMovementServiceImpl`: `productId` → `variantId`;
      lock theo variant_id order.
- [x] `repository/StockLevelRepository`, `StockSummaryRepository`, `ReorderSuggestionRepository` +
      `repository/projection/*`: cột `product_id` → `variant_id` trong `@Query`.
- [x] `mapper/StockLevelMapper`, `StockMovementMapper`, `StockSummaryMapper`, stock DTOs: variant.
- [x] `messaging/StockUpdateConsumer`, `ReorderConsumer`, `StockEventPublisher`, `service/StockCacheKey`,
      `RedisStockCacheService`: key `stock:{warehouseId}:{variantId}`.
- [x] `controller/StockController`, `MovementController`: item theo variant.

### 2d. IAM / Security — bắt buộc
- [x] `security/SecurityConfig`: cập nhật RBAC (spec §8) — `hasRole("USER")` → `CUSTOMER`; thêm matcher
      cho STAFF/MANAGER (warehouse endpoints), giữ `RateLimitingFilter`.

### 2e. Config reconciliation
- [x] RabbitMQ: order/payment và stock topology đã merge, converter hỗ trợ Java time. Cầu export async
      không cần cho luồng hiện tại vì reserve/fulfill đang đồng bộ qua `InventoryFacade`.
- [x] Redis: có template chung và `stockRedisTemplate` riêng cho cache tồn kho.
- [x] `AlertEmailProperties`, `OpenApiConfig`: đã compile/boot với package/property hiện tại.

### 2f. Điểm nối Order ↔ Stock (mới) — cốt lõi
- [x] `InventoryFacade` interface + implementation xử lý `reserve`, `fulfill`, `release` theo variant
      và warehouse; ordering không đụng repository tồn kho.
- [x] Luồng hiện tại reserve lúc đặt, release khi hủy/payment decline và fulfill khi SHIPPED. Đây là
      lựa chọn đồng bộ thay cho consumer export.

### 2g. Tests
- [x] Test đã khớp model variant; có concurrent oversell test và test thiếu hàng trước khi tạo order.
- [x] Unit test, compile/test-compile và app boot đã được xác minh ở các progress log mới hơn.

**Lệnh kiểm tra tiến độ Stage 2:**
```bash
cd Marketplace/backend
./mvnw -q clean compile      # sửa dần tới khi hết lỗi
./mvnw -q clean test
```

---

## 🎨 Stage 3 — ĐÃ XONG: gộp frontend (Angular) (2026-07-27)

Base frontend = OrderFlow (features: products, cart, orders, admin, auth, dashboard, users) + StockPulse.
- [x] Copy & xây dựng các feature StockPulse: `warehouses` (Danh sách & Form quản lý nhà kho), `stock` (`StockListComponent` theo dõi tồn kho/mức tồn kho/cảnh báo tồn kho), `movements` (`MovementListComponent` + `MovementFormComponent` tạo phiếu Nhập/Xuất/Chuyển kho).
- [x] Sửa models (`product`, `cart`, `order`, `category`) & services (`product`, `cart`, `warehouse`, `stock`) khớp DTO backend theo mô hình **ProductVariant (SKU)** & `variantId`.
- [x] `app.routes.ts` + navigation sidebar `layout/admin-layout`: Thêm cụm menu Warehouse & Stock Management.
- [x] Angular build (`ng build`): **BUILD SUCCESS** (0 TypeScript / Template errors).

---

## Cách chạy (sau khi Stage 2 xong)
```bash
cd Marketplace/backend && docker compose up -d      # postgres(5433)+redis+rabbitmq+mailhog
./mvnw clean spring-boot:run                          # API :8080, Swagger /swagger-ui.html
cd ../frontend && npm install && npm start            # Angular :4200
```

### Product Reviews & Ratings + Cart Fix production pass — 2026-07-28

- **Backend**:
  - Full Product Reviews & Ratings REST APIs (`/api/v1/reviews/**`, `/api/v1/admin/reviews/**`) with eligibility verification (verified purchase), double-review constraint, rating summary calculations, and soft-delete/visibility toggling (`REQ-STP-B-101` → `REQ-STP-B-108`).
  - Added `@JsonIgnoreProperties(ignoreUnknown = true)` to `CartItemResponse` / `CartResponse` and String JSON deserialization handling in `CartServiceImpl` to fix Redis Jackson default-typing `Unrecognized field "@class"` 500 error on Add to Cart.
- **Frontend (Angular)**:
  - Created `RatingSummaryComponent` (`REQ-STP-F-101`), `ReviewListComponent` (`REQ-STP-F-102`), and `ReviewFormComponent` (`REQ-STP-F-103`).
  - Integrated eligibility check (`REQ-STP-F-104`), `Verified Purchase` badge & edit badges (`REQ-STP-F-105`), and loading/empty/error/retry UI state handling (`REQ-STP-F-106`).
  - Upgraded Product Detail view (`/products/:id`) with variant selection, live price update, quantity stepper, and Add to Cart / Buy Now actions. Added unauthenticated redirect in `ProductListComponent`.
- **Testing**:
  - Full test suite passing: Unit tests for rating boundary 1-5, rating summary calculations, double-review prevention, component unit tests, and Playwright E2E tests (`REQ-STP-T-101` → `REQ-STP-T-106`). Verified: backend 75/75 unit tests pass, Angular build succeeds cleanly.

Tài khoản seed (mật khẩu `admin123`): `admin` / `manager` / `staff` / `customer`.

### Recommendation event schema foundation — 2026-07-28

- ✅ Hoàn thành `REQ-STP-B-501`: canonical analytics schema v1 cho `PRODUCT_VIEW`,
  `RECOMMENDATION_IMPRESSION`, `RECOMMENDATION_CLICK`, `ADD_TO_CART`, `PURCHASE`.
- ✅ Bổ sung typed source/placement/strategy, `eventId`, `schemaVersion`, UTC `Instant`,
  SPU `productId`, SKU `variantId` và recommendation correlation context.
- ✅ Giữ tương thích các event cũ (`PAGE_VIEW`, `SEARCH`, `ADD_TO_WISHLIST`,
  `BEGIN_CHECKOUT`, `ORDER_CREATED`) và chữ ký Angular `track(type, properties)`.
- ✅ Contract được tài liệu hóa tại `docs/analytics-event-schema-v1.md`; persistence,
  validation và deduplication vẫn thuộc `REQ-STP-B-502`/`REQ-STP-B-503`.
- ✅ Verify: backend unit tests **87/87 PASS**, Angular unit tests **8/8 PASS**,
  Angular production build **SUCCESS**.

### Recommendation event ingestion — 2026-07-28

- ✅ Hoàn tất phần code `REQ-STP-B-502`: `POST /api/v1/analytics/events` lưu raw event
  qua `AnalyticsEventService` → `AnalyticsEventRepository` vào bảng `analytics_events`.
- ✅ Hỗ trợ anonymous session và user từ JWT; không nhận user ID từ body. Response giữ
  các field cũ và bổ sung `eventId`, `ACCEPTED`, `receivedAt`.
- ✅ Giữ tương thích các frontend call cũ bằng cách tự sinh UUID và promote
  `productId`/`variantId`/`quantity` từ legacy `properties` khi cần.
- ✅ Public endpoint từ chối `PURCHASE`; loại event này chỉ dành cho trusted server flow.
  Log không còn in raw properties hoặc session ID.
- ✅ Migration mới: `V13__create_analytics_events.sql`; không sửa/đổi tên migration cũ.
- ✅ Verify code: focused analytics tests **8/8 PASS**; toàn bộ backend unit tests
  **91/91 PASS**.
- ✅ Đóng `REQ-STP-B-502` theo phạm vi đã thống nhất: implementation, HTTP contract và
  unit/controller regression đã xanh. Docker/PostgreSQL migration execution không thuộc
  phạm vi xác minh của requirement này; không sửa/đổi tên các migration cũ.

### Recommendation event validation and deduplication — 2026-07-28

- ✅ Hoàn thành `REQ-STP-B-503`: validate schema/event type, product SPU, variant SKU,
  recommendation placement context và timestamp tại API/service boundary.
- ✅ Product và variant phải tồn tại, đang active; variant phải thuộc đúng product.
  `ADD_TO_CART` bắt buộc có variant và số lượng dương.
- ✅ Recommendation impression/click bắt buộc có source, placement, request ID, strategy,
  position; strategy phải phù hợp với placement. Browser không được gửi `PURCHASE`.
- ✅ Timestamp chỉ được lệch tương lai tối đa 5 phút và không cũ quá 7 ngày.
- ✅ Chống ghi trùng atomically bằng unique `event_id` và
  `INSERT ... ON CONFLICT DO NOTHING`; retry trả `DUPLICATE_IGNORED` và không ghi đè
  event gốc.
- ✅ Verify: focused analytics tests **18/18 PASS**; toàn bộ backend unit tests
  **101/101 PASS**; Angular unit tests **8/8 PASS**; Angular production build
  **SUCCESS**. Không chạy Docker/PostgreSQL theo phạm vi đã thống nhất.

### Recommendation strategy contract — 2026-07-28

- ✅ Hoàn thành `REQ-STP-B-504`: thêm interface `RecommendationStrategy` tách thuật toán
  chọn/xếp hạng ứng viên khỏi controller, HTTP DTO và product response mapping.
- ✅ Thêm immutable internal contract `RecommendationContext` và
  `RecommendationCandidate`; strategy chỉ trả product ID, score và reason.
- ✅ Thêm `RecommendationStrategyRegistry` resolve implementation theo
  `RecommendationStrategyType`, fail-fast khi đăng ký trùng type và báo rõ type chưa
  được triển khai.
- ✅ Không thêm controller, repository, migration, frontend hoặc implementation giả;
  B-505, B-506 và B-507 có thể đăng ký strategy độc lập bằng Spring bean.
- ✅ Verify: strategy contract/registry tests **6/6 PASS**; toàn bộ backend unit tests
  **107/107 PASS**. Không cần Docker/PostgreSQL cho requirement này.

### Similar-product recommendation strategy — 2026-07-28

- ✅ Hoàn thành `REQ-STP-B-505`: `SimilarProductRecommendationStrategy` đăng ký type
  `SIMILAR` qua registry của B-504 và không phụ thuộc controller/API.
- ✅ Scoring xác định, có trọng số: category 35%, normalized brand 25%, Jaccard
  attributes 25%, khoảng giá min/max của SKU active 15%; tie-break theo product ID.
- ✅ Candidate phải có ít nhất một tín hiệu ngữ nghĩa category/brand/attribute; gần giá
  đơn thuần không đủ để xem là sản phẩm tương tự.
- ✅ Mở rộng catalog theo hướng additive với `products.brand` và JSONB `attributes`;
  create/update/response DTO giữ constructor cũ để không phá consumer hiện hữu. Giá vẫn
  chỉ nằm ở `product_variants`.
- ✅ Migration mới `V14__add_product_recommendation_metadata.sql`; không chỉnh sửa
  migration đã áp dụng. Contract/scoring được ghi tại
  `docs/similar-product-recommendation.md`.
- ✅ Verify: B-505 strategy/metadata tests **10/10 PASS**; focused impacted tests
  **33/33 PASS**; toàn bộ backend unit tests **117/117 PASS**; Angular unit tests
  **8/8 PASS**; Angular production build **SUCCESS**. Không chạy Docker/PostgreSQL.

### Best-seller recommendation strategy — 2026-07-29

- ✅ Hoàn thành `REQ-STP-B-506`: `BestSellerRecommendationStrategy` đăng ký type
  `BEST_SELLER` qua registry B-504; không thêm controller/API.
- ✅ Aggregate từ order item SKU về product/SPU; xếp hạng theo số đơn distinct hợp lệ,
  tie-break bằng tổng số lượng bán rồi product ID. Hỗ trợ category filter và limit.
- ✅ Mặc định chỉ tính `CONFIRMED`, `PROCESSING`, `SHIPPED`, `DELIVERED` trong 30 ngày;
  lookback và status cấu hình qua application properties/environment.
- ✅ Configuration được validate fail-fast: lookback tối thiểu một ngày,
  `PENDING`/`CANCELLED` không thể được xem là đơn bán hợp lệ.
- ✅ Thêm index hỗ trợ aggregation tại
  `V15__add_best_seller_query_indexes.sql`; không sửa migration đã áp dụng.
- ✅ Contract/ranking được ghi tại `docs/best-seller-recommendation.md`.
- ✅ Verify: B-506 strategy/configuration tests **10/10 PASS**; toàn bộ backend unit
  tests **127/127 PASS**. B-506 không đổi frontend; không chạy Docker/PostgreSQL.

### Co-occurrence recommendation strategies — 2026-07-29

- ✅ Hoàn thành `REQ-STP-B-507`: `CoViewedRecommendationStrategy` và
  `CoPurchasedRecommendationStrategy` đăng ký độc lập qua registry B-504; không thêm
  controller/API.
- ✅ `CO_VIEWED` đếm viewer distinct theo session (ưu tiên) hoặc user; loại lượt xem
  lặp cùng product của cùng viewer trước khi tính đồng xuất hiện.
- ✅ `CO_PURCHASED` aggregate từ SKU về product/SPU và đếm order distinct theo tập
  trạng thái đơn hợp lệ dùng chung với B-506.
- ✅ Cả hai strategy loại chính source, chỉ lấy product active có ít nhất một variant
  active, dùng lookback 90 ngày và ngưỡng đồng xuất hiện tối thiểu 2 có thể cấu hình.
- ✅ Bổ sung placement `PRODUCT_DETAIL_CO_PURCHASED`, index truy vấn tại
  `V16__add_co_occurrence_query_indexes.sql` và contract tại
  `docs/co-occurrence-recommendation.md`.
- ✅ Verify: B-507 strategy/source validation tests **11/11 PASS**; focused related
  tests **32/32 PASS**; toàn bộ backend unit tests **141/141 PASS**; Angular unit tests
  **8/8 PASS**; Angular production build **SUCCESS**. Không chạy Docker/PostgreSQL.

### Recommendation eligibility filtering — 2026-07-29

- ✅ Hoàn thành `REQ-STP-B-508`: mọi strategy được resolve qua
  `RecommendationStrategyRegistry` đều được decorate bằng
  `RecommendationCandidateFilter` trước khi trả kết quả.
- ✅ Bộ lọc loại source product, product ID trùng, product hidden/discontinued
  (`active = false`) và product không còn SKU active; giữ nguyên thứ tự, score và reason
  từ strategy.
- ✅ Eligibility được tải bằng một batch query, không phát sinh truy vấn N+1; limit được
  áp dụng sau bước lọc.
- ✅ Không đổi controller, HTTP DTO, frontend, schema dữ liệu hoặc migration. Contract
  được ghi tại `docs/recommendation-eligibility-filter.md`.
- ✅ Verify: focused recommendation/filter tests **30/30 PASS**; toàn bộ backend unit
  tests **146/146 PASS**. Không chạy Docker/PostgreSQL.

### Recommendation storefront API — 2026-07-29

- ✅ Bổ sung public `GET /api/v1/recommendations` điều phối theo placement sang
  `SIMILAR`, `BEST_SELLER`, `CO_VIEWED` hoặc `CO_PURCHASED`.
- ✅ Response trả `requestId`, placement, strategy, generated time và position 0-based
  để frontend correlation impression/click; user lấy từ JWT, anonymous session lấy từ
  `X-Session-Id`.
- ✅ Hydrate product và active variants bằng batch query, giữ thứ tự strategy và kiểm
  tra lại eligibility trước response; empty result trả `items: []`.
- ✅ Validate context theo placement và giới hạn `limit` từ 1–24; OpenAPI DTO/schema và
  typed Angular `RecommendationService` đã đồng bộ. Chưa triển khai carousel/UI F-501.
- ✅ Contract được ghi tại `docs/recommendation-api.md`.
- ✅ Verify: focused API/orchestration/filter tests **20/20 PASS**; toàn bộ backend unit
  tests **155/155 PASS**; Angular unit tests **8/8 PASS**; Angular production build
  **SUCCESS**. Không chạy Docker/PostgreSQL.

### Recommendation storefront frontend — 2026-07-29

- ✅ Hoàn thành `REQ-STP-F-501` → `REQ-STP-F-506`: reusable
  `RecommendationCarouselComponent`, tích hợp similar/co-viewed trên product detail và
  best-seller trên storefront/category.
- ✅ Impression chỉ được gửi khi card đạt 50% vùng nhìn; click gửi đủ source, placement,
  request ID, strategy và position. Attribution được validate, chỉ consume một lần cho
  `PRODUCT_VIEW`, tiếp tục giữ journey context cho cart/wishlist và được xóa khi truy cập
  trực tiếp.
- ✅ Có skeleton, retry/error state, ẩn section khi API trả rỗng, horizontal scroll,
  keyboard controls, carousel/group semantics, ARIA labelling, screen-reader
  announcements, responsive layout và reduced-motion support.
- ✅ Product detail hủy request cũ khi Angular tái sử dụng route, bật scroll restoration
  và ngăn response của sản phẩm trước ghi đè state sản phẩm mới.
- ✅ Hoàn thành `REQ-STP-T-505` với tests cho render/accessibility, ngưỡng observer 50%,
  impression deduplication, click correlation, observer cleanup, attribution lifecycle,
  stale-response cancellation, empty result và keyboard navigation.
- ✅ Verify: focused recommendation frontend tests **10/10 PASS**; toàn bộ Angular unit
  tests **18/18 PASS**; Angular production build **SUCCESS**.
- ✅ Hoàn thành `REQ-STP-T-506`: Playwright mở source product, chờ impression ở ngưỡng
  hiển thị, click recommendation, xác nhận attributed `PRODUCT_VIEW`, add SKU vào cart
  và kiểm tra đúng thứ tự `IMPRESSION → CLICK → PRODUCT_VIEW → ADD_TO_CART`.
- ✅ Playwright tự khởi động Angular dev server và dùng API route mocks xác định; toàn bộ
  E2E hiện có **3/3 PASS**.

### Workstream 3 testing completion — 2026-07-30

- ✅ Đồng bộ checklist `REQ-STP-T-501` → `REQ-STP-T-506`; toàn bộ requirement backend,
  frontend và testing của FEATURE-STP-03 đã hoàn thành.
- ✅ Focused backend recommendation/event tests **64/64 PASS**.
- ✅ Toàn bộ backend unit tests (không gồm IntegrationTest) **222/222 PASS**.
- ✅ Focused Angular recommendation tests **10/10 PASS**; toàn bộ Angular unit tests
  **26/26 PASS**.
- ✅ Angular production build **SUCCESS**; còn một budget warning đã có sẵn ở
  `product-list.component.ts`, không chặn build.
- ✅ Playwright E2E **3/3 PASS**, gồm recommendation attribution journey, review flow và
  customer funnel journey.
- ℹ️ Frontend test tooling yêu cầu Node.js **20.9+** vì Playwright 1.62 không còn hỗ trợ
  Node.js 18.

### FEATURE-03 Product Reviews — siết anti-fake-review (linkage GatePay↔MarketPlace) — 2026-08-04

- ✅ **Backend Phase 1+2** (kế hoạch: `docs/feature-03-product-reviews-plan.md`): chỉ cho đánh giá sản
  phẩm **đã mua & đơn DELIVERED**, đúng spec `FEATURE_03_PRODUCT_REVIEWS.md` của PayGate.
  - `OrderItemRepository.findEligibleOrderItemsForReview`: lọc `status = DELIVERED` (trước là `!= CANCELLED`).
  - `ReviewServiceImpl.createReview`: **chặn** người chưa mua/chưa nhận hàng (`ForbiddenException` 403),
    bỏ nhánh tạo review `orderItem == null`; mọi review là verified purchase; `orderItemId` phải thuộc
    tập eligible của user. `checkEligibility` chỉ `eligible=true` khi có đơn DELIVERED chưa review.
  - `ProductReview`: bỏ annotation `uk_user_product` (đã DROP ở DB; DB dùng partial index `uk_user_order_item`).
- ✅ Verify: `./mvnw test -Dtest='!*IntegrationTest'` = **270/270 PASS**; `test-compile` **BUILD SUCCESS**.
  `ReviewIntegrationTest` đã cập nhật theo hành vi mới nhưng **chưa chạy** (cần Docker).
- ✅ **Phase 4 (FE)**: nút "Review" (trang My Orders) chỉ hiện khi đơn **DELIVERED** + item chưa review
  (`order-list.hasUnreviewedItems`); dọn dead code review-form/eligibility ở `product-detail`.
- ✅ **Admin lọc review theo rating**: `GET /api/v1/admin/reviews` (filter `rating` 1–5 / `status` / `productId`
  qua `JpaSpecificationExecutor`; MANAGER/ADMIN) + trang admin `/admin/reviews` (bảng + filter + Hide/Approve).
- ✅ **Nâng cấp "đúng thực tế" G1–G7** — migration `V20260804120000` (`orders.delivered_at` + backfill,
  `product_reviews.seller_reply/seller_reply_at`):
  - **G1** sao + số review trên card sản phẩm (`GET /api/v1/products/ratings?ids=`).
  - **G2** đánh giá chỉ-sao (`title`/`content` optional, lưu "" nếu trống).
  - **G3** khách tự "Đã nhận hàng" (`PUT /api/v1/orders/{id}/confirm-received`, SHIPPED→DELIVERED, set `delivered_at`).
  - **G4** shop trả lời review (`PUT /api/v1/admin/reviews/{id}/reply`; hiển thị "Shop response").
  - **G5** thời hạn đánh giá `marketplace.review.window-days` (mặc định 90) tính từ `delivered_at`.
  - **G6** khách sửa review **1 lần** (admin không giới hạn).
  - **G7** hiện `variantName`/`sku` (mua mẫu nào) trong review.
  - *Không làm (ngoài phạm vi MarketPlace):* thưởng-điểm-review (GatePay Loyalty), auto-lọc-spam,
    xử-lý-review-khi-hoàn-tiền (MarketPlace chưa có luồng refund).
- ✅ Verify (mới nhất): `./mvnw test -Dtest='!*IntegrationTest'` = **272/272 PASS**; `npm run build` = **SUCCESS**.

### Review hardening follow-up — 2026-08-03

- ✅ #B: refresh JWT có `jti`; Redis giữ token hiện hành theo `refresh:{username}`; refresh
  compare-and-rotate nguyên tử, logout compare-and-revoke. Angular gọi revoke trước khi kết thúc phiên.
- ✅ #D: `PaymentConsumer` không còn mở transaction quanh gateway call và không còn biến mọi exception
  thành payment failure. Gateway outage được ném lại để Rabbit retry/DLQ.
- ✅ Payment ledger `payment_attempts` chống xử lý terminal event lặp, lưu provider transaction và hỗ trợ
  `REFUND_REQUIRED`/idempotent refund khi order bị hủy trong lúc payment đang xử lý.
- ✅ Payment decline cập nhật order + release reservation + refund coupon trong một transaction service có
  pessimistic order lock; rollback cùng nhau nếu compensation lỗi.
- ✅ #E đã đúng và có regression test: reserve thiếu hàng throw trước khi order được lưu/publish.
- ✅ #G: sửa trạng thái tổng, checklist Stage 2 và link README để khớp code thực tế.
- ✅ #H: thêm `PaymentReconciliationIntegrationTest` cho approved/declined và Rabbit retry. Test compile;
  lần chạy 2026-08-03 skip 2/2 vì không tìm thấy Docker daemon.
- ⚠️ #C: bỏ hoàn toàn `simulatePaymentGateway()`. Contract `PaymentGateway` và safe default `disabled`
  đã có; adapter thật còn chờ team chọn provider và bổ sung payment method/token vào checkout.
- ✅ Verify: backend unit test **255/255 PASS**; Angular unit test **39/39 PASS**; Angular production build
  **SUCCESS** (2 CSS budget warnings); backend payment integration **2 SKIPPED** do Docker không khả dụng.

### Delivery tracking — 2026-08-04

- ✅ Thêm manual delivery tracking cho order `SHIPPED`: carrier, tracking code, ETA, state machine
  `PENDING → IN_TRANSIT → DELIVERED/FAILED` và timeline event chi tiết.
- ✅ Migration duy nhất `V20260804101500__create_delivery_tracking.sql` tạo `deliveries` và
  `delivery_events`, gồm ownership FK, unique order/tracking, optimistic version, request-id
  idempotency và timeline index.
- ✅ Delivery là nguồn chân lý cho bước `SHIPPED → DELIVERED`; admin không còn chuyển order trực
  tiếp. Delivery service khóa theo thứ tự `Order → Delivery` và cập nhật delivery/event/order trong
  cùng transaction; inventory vẫn fulfill tại thời điểm order chuyển `SHIPPED`.
- ✅ API customer ownership và back-office STAFF/MANAGER/ADMIN đã có; metadata chỉ sửa khi
  `PENDING`, milestone request retry không tạo trùng.
- ✅ Status command mang `expectedVersion`; command cũ bị từ chối bằng HTTP `409 Conflict` sau
  khi khóa delivery. Query kiểm tra tracking dùng đúng biểu thức functional index
  `LOWER(carrier), UPPER(tracking_code)`.
- ✅ Angular có typed delivery service, customer timeline với loading/error/retry, và admin form
  tạo/sửa delivery, ghi milestone, cập nhật trạng thái.
- ✅ Verify sau khi merge `origin/dev`: focused backend delivery/order/security tests **22/22 PASS**;
  toàn bộ backend unit test **287/287 PASS**; toàn bộ Angular unit test **50/50 PASS**;
  Angular production build **SUCCESS**
  (giữ nguyên 2 CSS budget warnings có sẵn).
- ⚠️ `DeliveryTrackingIntegrationTest` đã thêm để kiểm tra concurrent create, normalized tracking
  query và migration thật; môi trường hiện tại không có lệnh/Docker daemon nên chưa thể thực thi
  Testcontainers.

### FEATURE-STP-02 Week 2 — Merchandising storefront + tests — 2026-08-04

- ✅ Nhánh `feature/stp-02-campaigns-collections-merchandising` **đã merge `dev`** (ngang dev, +32 commit); backend compile + FE build lại **xanh** sau merge (auto-merge sạch cả ngữ nghĩa).
- ✅ **F-404** storefront render (chỉ từ dữ liệu backend, hiệu lực do server quyết): `MerchandisingBannerComponent` (slot HOME_HERO), `CampaignStripComponent` (**surface** coupon — D-1 không auto-apply), `CollectionShowcaseComponent` (sản phẩm theo `display_order`), gộp trong `StorefrontMerchandisingComponent` nhúng ở trang `/products`.
- ✅ **F-405** analytics hooks: `MerchandisingImpressionDirective` (IntersectionObserver ≥50%, fire **1 lần**) + `MerchandisingService.recordEvent` (impression khi hiển thị, click **trước** điều hướng; idempotent theo `eventId`, dedup server-side).
- ✅ **F-406** effectiveness table (ADMIN): `MerchandisingEffectivenessComponent` gọi `GET /api/v1/admin/merchandising/summary` → impressions/clicks/CTR/attributed orders; thêm route `admin/merchandising/effectiveness` + link sidebar.
- ✅ Tests: **T-401** window validation + **T-404** event dedup/CTR/last-click attribution (`CampaignServiceImplTest`, `MerchandisingEventServiceImplTest`, +14 unit); **T-405** Angular (`merchandising.service.spec`, `campaign-strip.component.spec`, +7).
- ✅ Verify: backend unit **270/270 PASS**; Angular unit **46/46 PASS**; `npm run build` **SUCCESS** (2 CSS budget warnings cũ).
- ⏳ Còn lại (Docker/Playwright-gated — khớp quyết định CI hiện chỉ chạy unit test): **T-402/T-403** integration (CRUD/publish + reorder-in-1-transaction), **T-406** E2E; và PO chốt **D-1/D-5**.
- ▶️ **Sẵn sàng mở PR nhánh → `dev`.**

### confirm-received (F03/G3) đồng bộ với Delivery tracking (F02) — 2026-08-04

> Có **2 đường** đưa order tới `DELIVERED`: nút "Đã nhận hàng" của khách (F03/G3) và delivery→DELIVERED (F02).
> Chúng chưa đồng bộ → khách xác nhận sớm làm bản ghi Delivery **mồ côi/kẹt** (order DELIVERED nhưng delivery vẫn IN_TRANSIT, và admin không cập nhật delivery được nữa do chốt "chỉ đổi khi order SHIPPED"). Đã sửa (① + ②).
> ⚠️ Đụng `DeliveryService`/`DeliveryServiceImpl` (feature của Giảng) — **cần Giảng + Vinh review** khi PR.

- **① Sync:** `OrderServiceImpl.confirmReceived` gọi `DeliveryService.completeForCustomerConfirmation(orderId, userId)` → nếu order có Delivery thì **đóng luôn delivery = DELIVERED** (+ event "Khách xác nhận đã nhận hàng") trong **cùng transaction**; lock theo thứ tự `Order → Delivery` (order dùng `findByIdForUpdate`). Order + delivery luôn nhất quán.
- **② Gate:** chỉ cho xác nhận khi delivery đã **`IN_TRANSIT`** (đã lấy hàng). Delivery `PENDING` → BE ném lỗi rõ ràng, FE `canConfirmReceived()` ẩn nút; **không có** delivery record → vẫn cho (fallback đơn SHIPPED không tạo tracking); đã `DELIVERED` → idempotent.
- ✅ Verify: backend unit **293/293 PASS**; `npm run build` **SUCCESS**.

### webhook security hardening (F00 — Khoa) — 2026-08-06

> Vá + siết `PaymentWebhookServiceImpl` (nhánh `fix/webhook-security-hardening`). **Đụng code webhook đã merge của Hoàng** (PR #46 `fix/webhook-phantom-stock`) — bản này **giữ** phần nhận diện hủy của Hoàng và **bổ sung** các lớp bảo mật + concurrency.
> ⚠️ Chạm code đã merge của Hoàng → **cần Hoàng + Vinh review** khi PR.

- **Chữ ký:** thiếu chữ ký → **403** (mặc định `require-signature=true`); so khớp exact + constant-time (`MessageDigest.isEqual`) → đóng lỗ `.contains()` bypass + timing side-channel. Cấu hình `marketplace.paygate.webhook.require-signature` (env `PAYGATE_WEBHOOK_REQUIRE_SIGNATURE`).
- **Idempotency + concurrency:** đọc đơn bằng `findByIdForUpdate` (SELECT..FOR UPDATE) → check + nhả/trừ kho **atomic**, chặn double-release khi PayGate retry đồng thời (phantom stock). Guard `paymentStatus==PAID || status==CANCELLED` (bền khi đơn tiến sang SHIPPED, không hụt như `status==CONFIRMED`).
- **Fail-fast prod:** `WebhookSignatureConfigGuard` từ chối khởi động app nếu profile `prod` mà `require-signature=false` → không thể lên air với webhook không xác thực.
- **Gộp của Hoàng:** nhận diện hủy qua `event` (`PAYMENT_CANCELLED`/`PAYMENT_FAILED`), không chỉ `status`.
- ✅ Verify: backend unit **309/309 PASS**.

### Shopping chat assistant MVP — 2026-08-09

- ✅ Spring Boot orchestration với Gemini `LlmGateway`-style adapter và deterministic fallback; không
  khôi phục Python microservice cũ.
- ✅ Hỗ trợ best seller, tư vấn theo query/category/budget/brand/attributes và campaign voucher; chỉ
  trả sản phẩm/SKU active còn tồn khả dụng.
- ✅ Campaign offer kiểm tra đồng thời campaign, promotion window, global usage, per-user usage và
  PRODUCT/CATEGORY/CART scope trước khi hiển thị.
- ✅ Anonymous conversation owner-bound qua `X-Session-Id`, authenticated qua JWT; lưu Redis TTL 24h.
- ✅ Angular widget trả product/voucher card, quick replies, điều hướng product detail và copy voucher;
  không tự thêm giỏ hoặc tự áp voucher.
- ✅ Analytics: `CHAT_MESSAGE`, `CHAT_PRODUCT_IMPRESSION`, `CHAT_PRODUCT_CLICK`,
  `CHAT_VOUCHER_CLICK`; không lưu raw chat text.
- ✅ Contract và cấu hình: `docs/chat-assistant.md`, `backend/.env.example`.
- ✅ Verify: backend unit **322/322 PASS**; focused chat **7/7 PASS**; frontend **55/55 PASS**;
  Angular production build **SUCCESS**. `ChatDataFoundationIntegrationTest` đã compile nhưng bị
  **SKIP** vì máy kiểm thử không có Docker daemon khả dụng.

### Shopping chat PayGate card checkout — 2026-08-10

- ✅ Chat checkout hỗ trợ thêm `CREDIT_CARD` với nhãn **PayGate E-Wallet / Card Gateway** bên cạnh COD.
- ✅ Tái sử dụng `OrderService` và PayGate checkout session hiện hữu: chatbot trả payment URL trong
  order card, mở PayGate ở tab mới và giữ hội thoại ở tab Marketplace.
- ✅ Kết quả chat không tin redirect query parameter; frontend poll order thuộc user và chỉ báo thành
  công khi webhook đã cập nhật `paymentStatus=PAID`, hoặc thất bại/hủy khi order thành `CANCELLED`.
- ✅ Pending payment được lưu local để tự theo dõi lại sau reload/callback; PayGate/MarketPlace vẫn nối
  qua API + signed webhook, không thêm coupling hoặc schema mới bên GatePay.

### MP-H2/M7 client auth hardening — 2026-08-11

- ✅ Backend trả refresh token bằng cookie `HttpOnly`, `SameSite=Lax`, path `/api/v1/auth`; response JSON
  không còn lộ `refreshToken`. Login/register/refresh rotate cookie, logout revoke Redis token và xóa cookie.
- ✅ Angular chỉ giữ access token/username/role trong bộ nhớ; `localStorage` chỉ giữ cờ phiên không nhạy cảm
  để quyết định có khôi phục phiên bằng cookie khi reload hay không. Dữ liệu auth từ build cũ được dọn.
- ✅ Refresh dùng một observable single-flight dùng chung cho các request 401 đồng thời; cả request thành công
  lẫn thất bại đều kết thúc, không còn subscriber chờ vô hạn.
- ✅ Local demo giữ `AUTH_REFRESH_COOKIE_SECURE=false`; production HTTPS phải bật `true`.
- ✅ Verify: backend unit **387/387 PASS**; Angular unit **79/79 PASS**; Angular production build
  **SUCCESS** (giữ nguyên 2 CSS budget warnings và 1 CommonJS warning có sẵn).

### MP-H4 back-office authorization hardening — 2026-08-11

- ✅ Đóng lỗ hổng `DELETE /api/v1/categories/{id}` từng rơi xuống `authenticated()` và cho phép mọi
  tài khoản đăng nhập đi tới thao tác xóa; hiện chỉ `ADMIN` được phép ở cả request matcher và method level.
- ✅ Bổ sung `@PreAuthorize` defense-in-depth cho catalog/user mutation và các controller Campaign,
  Collection, Banner, Merchandising; backend tiếp tục là security boundary độc lập với Angular guard.
- ✅ Angular admin routes khai báo role contract theo từng màn hình và `canActivateChild` thực thi contract;
  STAFF/MANAGER không còn điều hướng vào màn hình mà API tương ứng chỉ dành cho ADMIN.
- ✅ Regression matrix kiểm tra anonymous **401**, sai role **403**, đúng role **2xx** và không gọi service
  khi bị từ chối.
- ✅ Verify: backend unit **405/405 PASS**; Angular unit **84/84 PASS**; Angular production build
  **SUCCESS** (giữ nguyên 2 CSS budget warnings và 1 CommonJS warning có sẵn).

### PayGate card + VietQR webhook reconciliation fix — 2026-08-11

- ✅ GatePay local webhook dispatch now allowlists only the configured internal host (`localhost` by default),
  while the `prod` profile fails fast unless that allowlist is empty. This preserves SSRF blocking for all other
  loopback/private/link-local targets.
- ✅ Marketplace accepts the current `X-PayGate-Signature` contract and the legacy `X-Signature` header during
  rolling upgrades. GatePay signs the exact raw webhook body with the merchant API key.
- ✅ VietQR dev confirmation is signed server-to-server with `PAYGATE_BANK_WEBHOOK_SECRET`; the obsolete browser
  HMAC code and hard-coded client-side secret were removed.
- ✅ Verify: Marketplace backend unit **408/408 PASS**; Angular unit **84/84 PASS** and production build
  **SUCCESS**. GatePay focused webhook tests **11/11 PASS**; its pre-existing full suite remains red in unrelated
  legacy controller/service tests.
