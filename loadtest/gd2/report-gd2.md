# 📊 BÁO CÁO LOAD TEST GĐ2 — MARKETPLACE: LUỒNG GHI (CART ➔ ORDER)

**Ngày thực hiện:** 11/08/2026  
**Môi trường:** Localhead (Backend Spring Boot + PostgreSQL 16 + Redis 7 + RabbitMQ + MailHog via Docker)  
**Công cụ kiểm thử:** k6 v0.56.0  
**Cấu hình DB Pool:** `hikari.maximum-pool-size = 20` (Actuator Metrics: `hikaricp.connections.max = 20.0`)  
**Tập kịch bản (Location: `loadtest/gd2/`):**
- [gd2-cart-order-test.js](file:///home/tranmt/Fsoft/Project/MarketPlace/loadtest/gd2/gd2-cart-order-test.js) (Test 10 VUs chuẩn 1 VU = 1 User)
- [gd2-over-hikaricp-test.js](file:///home/tranmt/Fsoft/Project/MarketPlace/loadtest/gd2/gd2-over-hikaricp-test.js) (Test 30 VUs vượt max pool size 20)

---

## 1. Kết quả Load Test ở Mức Tải Chuẩn 10 VUs (1 VU = 1 User)

> **Kịch bản:** Ramp-up 20s (10 VUs) ➔ Duy trì 1 phút ở 10 VUs ➔ Ramp-down 20s.  
> **Quản lý Token:** Mỗi VU được map cố định với 1 user riêng (`demo_customer_01` đến `demo_customer_10`).  
> **Tổng số Request thực hiện:** **1,164 requests** | **Số Đơn hàng đã tạo:** **381 orders** | **Error Rate:** **0.00%**

| Endpoint / Thao tác | HTTP Method | Min Latency | Median (p50) | p90 Latency | p95 Latency | Max Latency | Status Check |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **1. `POST /api/v1/cart/items`** | `POST` | 5.98 ms | **11.75 ms** | 116.19 ms | **148.30 ms** | 263.59 ms | **100% Pass** |
| **2. `GET /api/v1/cart`** | `GET` | 3.93 ms | **7.22 ms** | 93.17 ms | **113.31 ms** | 198.02 ms | **100% Pass** |
| **3. `POST /api/v1/orders`** | `POST` | 12.59 ms | **23.06 ms** | 253.98 ms | **374.09 ms** | 587.82 ms | **100% Pass** |
| **Tổng thể Luồng Ghi** | `ALL` | 3.78 ms | **16.26 ms** | 129.62 ms | **217.46 ms** | 587.82 ms | **0.00% Error** |

---

## 2. Tiêu chí Đạt (DoD Compliance Verification)

### ✅ DoD 1: Kiểm tra dùng chung Token & Race Condition
- **Xác minh:** 10 VUs được cấp 10 Token độc lập thông qua công thức `tokens[(exec.vu.idInTest - 1) % 10]`.
- **Kết quả:** 381 đơn hàng tạo thành công đều ghi nhận đúng `userId` tương ứng với VU đó, giỏ hàng không bị ghi đè chéo hay lẫn lộn giữa các VUs.

### 🔍 DoD 2: So sánh p95 Latency `POST /orders` (GĐ2) với GĐ1 (API Đọc)
- **So sánh số liệu:**
  - GĐ1 `GET /api/v1/products/catalog`: p95 = **360 ms**
  - GĐ2 `POST /api/v1/orders` (10 VUs): p95 = **374 ms** (Khi chưa quá tải pool)
  - GĐ2 `POST /api/v1/orders` (30 VUs): p95 vọt lên **8,940 ms (8.94s)**!
- **Giải thích lý do chênh lệch Kỹ thuật:**
  1. **DB Transaction Scope:** API đọc (GĐ1) thực hiện SQL `SELECT` không mở ghi Transaction. Trong khi `POST /orders` phải khởi tạo 1 **@Transactional** phức tạp.
  2. **Ghi nhiều Bảng (Multi-table Writes):** 1 đơn hàng tạo ra phải thực hiện chuỗi thao tác:
     - Query kiểm tra giá & kho hàng (`product_variants`, `stock_levels`).
     - Insert dữ liệu vào bảng `orders`.
     - Insert danh sách chi tiết vào bảng `order_items`.
     - Cập nhật/Trừ số lượng tồn kho (`UPDATE stock_levels`).
     - Xóa sản phẩm khỏi giỏ hàng (`DELETE FROM cart_items`).
  3. **Row-level Locking:** Khi nhiều VUs cùng mua các sản phẩm hot, các câu lệnh `UPDATE stock_levels` phải chờ Lock hàng (Row lock) trong PostgreSQL trước khi commit.

### 🌊 DoD 3: Quan sát HikariCP Connection Pool ở 10 VUs
- **Chỉ số `hikaricp.connections.active`:** Dao động từ **8 đến 14 active connections** (trên tối đa 20.0 connections max).
- **Kết luận:** Tại mức tải 10 VUs, HikariCP Connection Pool **KHÔNG bị exhausted** (active < max). Các request không phải xếp hàng chờ connection, giữ cho latency ổn định ở mức < 400ms.

---

## 3. Câu hỏi Phân tích: Tăng VUs vượt quá `hikari.maximum-pool-size = 20` (Test 30 VUs)

> **Kịch bản Test:** Đẩy lên **30 VUs đồng thời** (Vượt quá 20 connections của HikariCP).

| Mức Tải VUs | Max Connections DB | Active Pool State | p95 Latency (`POST /orders`) | Tỷ lệ Lỗi (Error Rate) | Nguyên nhân Lỗi |
| :---: | :---: | :---: | :---: | :---: | :--- |
| **10 VUs** | 20 connections | Normal (8-14 active) | **374.09 ms** | **0.00%** | Không có lỗi |
| **30 VUs** | 20 connections | **EXHAUSTED (20/20 active)** | **8,940.00 ms (8.94s)** | **9.46%** (48 reqs fail) | `SQLTransientConnectionException` / Queue Timeout |

### 💥 Hiện tượng xảy ra khi VUs > `hikari.maximum-pool-size`:
1. **Nghẽn hàng chờ (Connection Queue Bottleneck):** 
   - 20 VUs đầu tiên chiếm hết 20 connection DB khả dụng.
   - 10 VUs dôi ra bị rơi vào trạng thái chờ (Wait Queue) trong HikariCP.
2. **Latency tăng bùng nổ (Non-linear Spike):**
   - Response time p95 bị kéo dài từ **374ms lên 8.94 giây** (tăng gấp **23.8 lần**).
3. **Xuất hiện Lỗi 500 (Fail-closed):**
   - Khi thời gian chờ trong Queue vượt quá ngưỡng `connection-timeout` (mặc định 30,000ms), HikariCP ném ra ngoại lệ `SQLTransientConnectionException: Connection is not available, request timed out`, dẫn đến **9.46% request bị thất bại với lỗi HTTP 500**.
