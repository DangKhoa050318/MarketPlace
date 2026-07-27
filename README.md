# Marketplace

Cửa hàng bán lẻ trực tuyến tự vận hành kho — hợp nhất **OrderFlow** (storefront: catalog, giỏ hàng,
đơn hàng, thanh toán) và **StockPulse** (kho: tồn kho đa kho, nhập/xuất/chuyển, cảnh báo, gợi ý nhập
lại) thành **một modular monolith** (Spring Boot + Angular + PostgreSQL/Redis/RabbitMQ/MailHog).

- Đặc tả: [`../Project - Marketplace (Merged Spec).md`](../Project%20-%20Marketplace%20(Merged%20Spec).md)
- Feature mới đề xuất: [`../Project - Marketplace (New Features Proposal).md`](../Project%20-%20Marketplace%20(New%20Features%20Proposal).md)
- **Trạng thái merge & việc còn lại:** [`MERGE-STATUS.md`](MERGE-STATUS.md) ← đọc file này trước khi code tiếp

## Kiến trúc đã chốt
Modular monolith · single-seller · catalog 2 tầng **SPU (`products`) → SKU (`product_variants`)**.
Tồn kho là nguồn chân lý ở `stock_levels` theo `(variant_id, warehouse_id)`; đặt đơn → trừ kho qua
`InventoryFacade` (pessimistic lock, chống oversell).

## Cấu trúc
```
Marketplace/
├── backend/    Spring Boot (base package com.training.marketplace), Flyway V1–V8
├── frontend/   Angular 17
├── README.md
└── MERGE-STATUS.md
```

## Chạy (dev)
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

> ⚠️ Đang trong quá trình merge — backend **chưa compile** cho tới khi hoàn tất Stage 2 trong
> [`MERGE-STATUS.md`](MERGE-STATUS.md). Chạy `docker compose down -v` một lần khi khởi tạo DB lần đầu.
