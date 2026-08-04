# Marketplace

Cửa hàng bán lẻ trực tuyến tự vận hành kho — hợp nhất **OrderFlow** (storefront: catalog, giỏ hàng,
đơn hàng, thanh toán) và **StockPulse** (kho: tồn kho đa kho, nhập/xuất/chuyển, cảnh báo, gợi ý nhập
lại) thành **một modular monolith** (Spring Boot + Angular + PostgreSQL/Redis/RabbitMQ/MailHog).

- Quy ước code cho toàn repo: [`AGENTS.md`](AGENTS.md)
- Đặc tả nền (đã merge): [`docs/Project - Marketplace (Merged Spec).md`](docs/Project%20-%20Marketplace%20(Merged%20Spec).md)
- Feature mới — nguồn chân lý: [`docs/StockPulse-Ecommerce-Features-3-Week-Requirements.md`](docs/StockPulse-Ecommerce-Features-3-Week-Requirements.md)
  (các đề xuất khác chỉ để tham khảo: [New Features Proposal](docs/Project%20-%20Marketplace%20(New%20Features%20Proposal).md), [TongHop](docs/StockPulse-TongHop-Features-Requirements.md))
- **Trạng thái merge & việc còn lại:** [`MERGE-STATUS.md`](MERGE-STATUS.md) ← đọc file này trước khi code tiếp

## Kiến trúc đã chốt
Modular monolith · single-seller · catalog 2 tầng **SPU (`products`) → SKU (`product_variants`)**.
Tồn kho là nguồn chân lý ở `stock_levels` theo `(variant_id, warehouse_id)`; đặt đơn → trừ kho qua
`InventoryFacade` (pessimistic lock, chống oversell).

## Cấu trúc
```
Marketplace/
├── backend/    Spring Boot (base package com.training.marketplace), Flyway (timestamp migrations)
├── frontend/   Angular 17
├── docs/       đặc tả (Merged Spec, MERGE-STATUS, requirements, feature specs)
├── AGENTS.md   quy ước code
└── README.md
```

## Chạy (dev)

Yêu cầu frontend: **Node.js 20.9+** (`@playwright/test` 1.62 không hỗ trợ Node.js 18).

```bash
cd backend && docker compose up -d          # postgres:5433, redis, rabbitmq, mailhog
./mvnw clean spring-boot:run                 # http://localhost:8080  (Swagger: /swagger-ui.html)
cd ../frontend && npm install && npm start   # http://localhost:4200
```
| Service | URL | |
|---|---|---|
| API | http://localhost:8080 | Swagger `/swagger-ui.html` |
| PostgreSQL | localhost:**5433** / `marketplace_db` | postgres / postgres |
| RabbitMQ | http://localhost:15672 | guest / guest |
| MailHog | http://localhost:8025 | |

Tài khoản seed (mật khẩu `admin123`): `admin`, `manager`, `staff`, `customer`.

> ⚠️ Seed chỉ dùng **local**. Khi deploy thật: đổi mật khẩu mạnh cho mọi tài khoản (đừng giữ `admin123`) và đặt `JWT_SECRET` **riêng cho từng môi trường** (app không còn giá trị default — bắt buộc set, xem `backend/.env.example`).

Refresh token được lưu server-side trong Redis, rotate sau mỗi lần refresh và revoke tại
`POST /api/v1/auth/logout`. Cấu hình payment mặc định là `disabled`: hệ thống không tự xác nhận
đơn chưa thanh toán; cần cài adapter `PaymentGateway` thật và đặt `PAYMENT_PROVIDER` sau khi chọn
nhà cung cấp.

> ℹ️ Backend/frontend đã compile & boot được (xem [`MERGE-STATUS.md`](MERGE-STATUS.md)).
> Chạy `docker compose down -v` một lần khi khởi tạo DB lần đầu.

## Flyway migrations — quy ước đặt tên (timestamp)

Đặt tên **mọi** migration theo mốc thời gian tạo file, **không** dùng số thứ tự:

```
V<yyyyMMddHHmmss>__<mo_ta>.sql      # vd: V20260729143000__add_campaign_tables.sql
```

- Timestamp giúp các nhánh feature làm song song **không trùng version** khi merge (kiểu `V1, V2…` trước đây liên tục đụng nhau).
- **Không sửa** migration đã apply — thêm file timestamp mới. Migration phải apply sạch trên DB mới (`docker compose down -v`).
- Đổi/renumber migration ⇒ **mọi người phải `docker compose down -v`** tạo lại DB (vì `flyway_schema_history` lưu version).
- Guard kiểm tra tự động: `bash backend/scripts/check-migration-versions.sh` (chạy trong CI — [`.github/workflows/backend-ci.yml`](.github/workflows/backend-ci.yml)) — fail nếu có version trùng hoặc còn kiểu `Vn` cũ.
