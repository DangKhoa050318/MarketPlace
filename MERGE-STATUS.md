# Marketplace — Merge Status & Continuation Guide

Tracks the merge of **OrderFlow** (storefront) + **StockPulse** (warehouse) into one modular
monolith, per `../Project - Marketplace (Merged Spec).md` (v0.2: monolith · single-seller ·
2-tier catalog with `product_variants`).

> **Trạng thái tổng: Stage 1 XONG (nền + schema). Backend CHƯA compile** — đây là trạng thái
> giữa chừng có chủ đích: schema & entity đã đổi sang mô hình variant, nhưng service/mapper/DTO/
> controller vẫn tham chiếu model cũ (Stage 2 sẽ reconcile). Danh sách file cần sửa liệt kê chính
> xác bên dưới.

---

### Customer experience event ingestion pipeline - 2026-07-29

- Done `REQ-STP-B-702`: `POST /api/v1/analytics/events` and
  `POST /api/v1/analytics/events/batch` ingest canonical storefront events.
- Validates `schemaVersion`, event type, timestamp window, required fields per event type,
  batch size 1-50, product existence, and add-to-cart variant/quantity.
- Deduplicates by client `eventId` with unique `analytics_events.event_id`; duplicate retry returns
  `DUPLICATE_IGNORED` without overwriting the original event.
- Persists raw event metadata in `analytics_events` via migration
  `V12__create_analytics_events.sql`; responses include per-event ingestion status:
  `ACCEPTED`, `DUPLICATE_IGNORED`, or `REJECTED`.
- Verify: focused analytics tests 4/4 PASS; backend non-integration unit suite 78/78 PASS;
  Angular production build SUCCESS. Local smoke test against running backend confirmed single
  event accepted, duplicate ignored, and batch accepted/rejected summary.

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
- [ ] 4 integration test (`*IntegrationTest`, Testcontainers) **compile xanh**; chạy bằng
      `docker compose up -d` rồi `./mvnw verify`. Chưa chạy đủ trong môi trường này.
- [ ] *(tuỳ chọn)* Cầu async `order.confirmed → stock.export.queue`: hiện làm **đồng bộ** trong
      `OrderServiceImpl` (reserve khi đặt, fulfill khi SHIPPED) — đúng & chống oversell tốt hơn.
- [ ] *(tuỳ chọn)* `jsonMessageConverter` (RabbitMQConfig) dùng ObjectMapper mặc định (không có JavaTimeModule);
      nếu publish event có `Instant`/`LocalDateTime` cần cân nhắc thêm module để tránh lỗi serialize lúc chạy.

Lệnh: `./mvnw -o clean compile` = **BUILD SUCCESS** · `./mvnw -o test-compile` = **BUILD SUCCESS** ·
`./mvnw -o test -Dtest='!*IntegrationTest'` = **62 pass**.

### 2a. Catalog (SPU/variant) — ✅ XONG
- [x] DTOs Product (SPU) + Variant (Create/Update/Response).
- [x] `ProductMapper` + `ProductVariantMapper`.
- [x] `ProductService`/`Impl` + `ProductVariantService`/`Impl` + `ProductVariantRepository`.
- [x] `ProductController` + `ProductVariantController`; `ProductRepository` (bỏ product-lock).
- [ ] `Category*`: `CategoryServiceImpl`/`CategoryMapper`/DTO thêm `code`+`parentId` (+ validate cycle
      như StockPulse `CategoryServiceImpl.validateParent`); merge với bản OrderFlow (slug). **CHƯA** — entity
      Category đã có code+slug+parentId nhưng DTO/mapper/service chưa set code+parentId (chạy được, dữ liệu thiếu).

### 2b. Ordering (khóa theo variant + tồn từ stock_levels) — bắt buộc
- [ ] `service/impl/OrderServiceImpl`: bỏ `product.getStock()/getPrice()`; giá lấy từ **variant**,
      tồn/khóa lấy từ **stock_levels** qua `InventoryFacade` (reserve/fulfill — §7 spec). Snapshot
      `sku/variantName` vào order_items.
- [ ] `service/impl/CartServiceImpl` + `CartItemResponse` + `AddToCartRequest`/`UpdateCartItemRequest`:
      cart key theo **variantId**, giá từ variant.
- [ ] `mapper/OrderMapper`, `dto/.../CreateOrderRequest`: theo variant.
- [ ] `consumer/PaymentConsumer`: tham chiếu productId → variant nếu cần.

### 2c. Inventory (StockPulse-origin → variant) — bắt buộc
- [ ] `service/impl/StockLevelServiceImpl`, `StockMovementServiceImpl`: `productId` → `variantId`;
      lock theo variant_id order.
- [ ] `repository/StockLevelRepository`, `StockSummaryRepository`, `ReorderSuggestionRepository` +
      `repository/projection/*`: cột `product_id` → `variant_id` trong `@Query`.
- [ ] `mapper/StockLevelMapper`, `StockMovementMapper`, `StockSummaryMapper`, stock DTOs: variant.
- [ ] `messaging/StockUpdateConsumer`, `ReorderConsumer`, `StockEventPublisher`, `service/StockCacheKey`,
      `RedisStockCacheService`: key `stock:{warehouseId}:{variantId}`.
- [ ] `controller/StockController`, `MovementController`: item theo variant.

### 2d. IAM / Security — bắt buộc
- [ ] `security/SecurityConfig`: cập nhật RBAC (spec §8) — `hasRole("USER")` → `CUSTOMER`; thêm matcher
      cho STAFF/MANAGER (warehouse endpoints), giữ `RateLimitingFilter`.

### 2e. Config reconciliation
- [ ] RabbitMQ: base = OrderFlow `RabbitMQConfig` (order/payment) + đã copy `StockRabbitTopology`
      (stock.*). Kiểm tra bean không trùng; thêm cầu **`order.confirmed → stock.export.queue`** (§9).
- [ ] Redis: OrderFlow `RedisConfig` là base; `RedisStockCacheService` có thể cần RedisTemplate riêng.
- [ ] `AlertEmailProperties`, `OpenApiConfig`: kiểm tra khớp package/property.

### 2f. Điểm nối Order ↔ Stock (mới) — cốt lõi
- [ ] Tạo `inventory/InventoryFacade` (interface) + impl: `reserve(variantItems, warehouseId)`,
      `fulfill(order)`, `release(order)`. `ordering` gọi facade này (không đụng repo inventory trực tiếp).
- [ ] Consumer `order.confirmed` → tạo phiếu EXPORT (link `source_order_id`) → complete → trừ tồn.

### 2g. Tests
- [ ] Sửa test copy từ 2 repo cho khớp model mới; thêm **concurrent oversell test trên 1 variant**.
- [ ] `./mvnw clean test` xanh; app khởi động (`ddl-auto: validate` pass với schema V1–V8).

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
Tài khoản seed (mật khẩu `admin123`): `admin` / `manager` / `staff` / `customer`.
