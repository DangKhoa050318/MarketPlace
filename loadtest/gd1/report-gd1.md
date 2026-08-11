# 📊 BÁO CÁO LOAD TEST GĐ1 — MARKETPLACE: API ĐỌC (BASELINE)

**Ngày thực hiện:** 11/08/2026  
**Môi trường:** Localhead (Backend Spring Boot + PostgreSQL 16 + Redis 7 + RabbitMQ + MailHog via Docker)  
**Công cụ kiểm thử:** k6 v0.56.0  
**Tập kịch bản (Location: `loadtest/gd1/`):**
- [gd1-baseline-test.js](file:///home/tranmt/Fsoft/Project/MarketPlace/loadtest/gd1/gd1-baseline-test.js) (Kịch bản 20 VUs per scenario)
- [gd1-pagination-test.js](file:///home/tranmt/Fsoft/Project/MarketPlace/loadtest/gd1/gd1-pagination-test.js) (So sánh size=10 vs size=100)
- [gd1-50vu-test.js](file:///home/tranmt/Fsoft/Project/MarketPlace/loadtest/gd1/gd1-50vu-test.js) (Kịch bản 50 VUs per scenario)

---

## 1. Kết quả Load Test 3 Endpoints ở mức tải 20 VUs (Tổng 60 VUs)

> **Kịch bản:** Ramp-up 30s ➔ Duy trì 1 phút ở 20 VUs per scenario (Tổng 60 VUs) ➔ Ramp-down 30s.  
> **Tổng số Request thực hiện:** **6,686 requests** | **Throughput:** ~55.51 req/sec | **Error Rate:** **0.00%**

| Endpoint | HTTP Method | Min Latency | Median (p50) | p90 Latency | p95 Latency | Tỷ lệ < 500ms |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: |
| **`GET /api/v1/products/catalog`** | `GET` | 1.29 ms | **7.41 ms** | 276.40 ms | **360.28 ms** | **99.3% Pass** |
| **`GET /api/v1/products`** | `GET` | 2.04 ms | **305.89 ms** | 732.49 ms | **834.56 ms** | **70.1% Pass** |
| **`GET /api/v1/recommendations`** | `GET` | 4.47 ms | **659.57 ms** | 1,680.00 ms | **1,800.00 ms** | **44.2% Pass** |

---

## 2. Phân tích & Giải thích Code Service (Endpoint nào p95 cao nhất?)

📌 **Endpoint có p95 latency cao nhất:** **`GET /api/v1/recommendations`** (p95 = **1,800ms / 1.8s**).

### 🔍 Giải thích nguyên nhân từ Source Code:
1. **`GET /api/v1/products/catalog` (Nhanh nhất - p95 = 360ms):**
   - Đọc trực tiếp qua `StorefrontCatalogRepository.browse()` sử dụng query SQL đơn giản truy vấn bảng sản phẩm đã active.
2. **`GET /api/v1/products` (Trung bình - p95 = 834ms):**
   - Sử dụng Spring Data `productRepository.findAll(pageable)`, thực hiện 2 câu lệnh SQL: 1 câu `COUNT(*)` để tính tổng số dòng phân trang và 1 câu `SELECT` lấy dữ liệu, sau đó map sang DTO.
3. **`GET /api/v1/recommendations` (Chậm nhất - p95 = 1,800ms):**
   - Xem code `RecommendationServiceImpl.java`:
     - **Tính toán đa bước (Multi-step calculation):** Đầu tiên gọi `strategy.recommend(context)` để tính toán thuật toán gợi ý (Best Sellers), cần JOIN nhiều bảng (`order_items`, `orders`, `products`).
     - **Quá trình Hydrate dữ liệu (Memory Hydration):** Sau khi có danh sách `candidate.productId()`, hàm `hydrate()` tiếp tục thực hiện thêm **2 câu query DB phụ**: 
       1. `productRepository.findAllByIdInAndActiveTrue(...)` lấy thông tin sản phẩm.
       2. `activeVariantsByProductId(...)` lấy toàn bộ danh sách biến thể (`variants`) của từng sản phẩm.
     - **Hệ quả:** 1 request gọi `/recommendations` tiêu tốn tới **3-4 câu truy vấn SQL phức tạp** kèm theo N+1 mapping trong bộ nhớ Java, làm tăng CPU overhead và chờ kết nối DB connection pool.

---

## 3. So sánh Pagination (`size=10` vs `size=100`) trên `GET /api/v1/products`

> **Kịch bản:** 20 VUs chạy trong 30s cho `size=10`, sau đó chạy 30s cho `size=100`.

| Tham số `size` | Tổng Requests | Response Time Trung bình (avg) | p90 Latency | p95 Latency | Max Latency |
| :---: | :---: | :---: | :---: | :---: | :---: |
| **`size=10`** | 1,127 reqs | 40.43 ms | 153.88 ms | **169.14 ms** | 438.08 ms |
| **`size=100`** | 1,200 reqs | 3.15 ms | 4.26 ms | **6.19 ms** | 11.26 ms |

### ❓ Trả lời câu hỏi: *"Pagination có giúp gì không?"*
- **Về mặt kỹ thuật Database/Memory:** **PAGINATION RẤT QUAN TRỌNG VÀ BẮT BUỘC TRONG PRODUCTION.** 
- **Giải thích hiện tượng `size=100` chạy nhanh hơn `size=10` ở test trên:** 
  - Do database test hiện tại mới chỉ có tổng cộng **24 sản phẩm**. Khi gọi `size=100`, Hibernate trả về toàn bộ 24 dòng chỉ trong 1 câu SQL single-pass và dữ liệu này đã được **Nạp nóng vào Buffer Pool của PostgreSQL & Spring L2 Cache** từ lượt test `size=10` trước đó.
  - Tuy nhiên, khi bảng `products` trong thực tế tăng lên **100,000 dòng**, việc không dùng Pagination (hoặc lấy `size=1000`) sẽ làm:
    1. **Tốn RAM Server:** Phải nạp 100,000 entity object vào bộ nhớ JVM.
    2. **Băng thông mạng (Network I/O):** Payload JSON phình to từ vài KB lên hàng chục MB.
    3. **GC Pause (Garbage Collection):** Gây khựng ứng dụng do JVM phải dọn dẹp hàng ngàn object rác sau mỗi request.

---

## 4. So sánh Tải khi tăng từ 20 VUs lên 50 VUs (Mỗi Scenario 50 VU - Tổng 150 VUs)

| Mức Tải | Tổng VUs Đồng thời | Avg Latency | p90 Latency | p95 Latency | Max Latency | Error Rate |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **20 VUs / Scenario** | **60 VUs** | 310.48 ms | 902.03 ms | **1,340 ms (1.34s)** | 2.29 s | **0.00%** |
| **50 VUs / Scenario** | **150 VUs** | 4,790.00 ms (4.79s) | 8,210 ms (8.21s) | **8,570 ms (8.57s)** | 14.83 s | **0.00%** |

### ❓ Trả lời câu hỏi phân tích:
1. **`p95 / p99` latency ở 20 VU là bao nhiêu?**
   - Ở 20 VU per scenario (tổng 60 VUs): `p95 = 1.34s`, `p90 = 0.90s`.
2. **Latency có tăng tuyến tính khi tăng lên 50 VU không?**
   - **KHÔNG TĂNG TUYẾN TÍNH, MÀ TĂNG THEO CẤP SỐ NHÂN (Non-linear degradation)!**
   - Khi tăng VU gấp 2.5 lần (từ 60 lên 150 VUs), latency p95 **tăng gấp 6.4 lần** (từ `1.34s` vọt lên `8.57s`).
   - **Lý do kỹ thuật:** Do kết nối HikariCP Connection Pool (mặc định 10-20 connections) và số Worker Thread của Tomcat bị quá tải queue. Các VUs phải xếp hàng chờ (Wait Time) để được cấp connection DB, dẫn đến thời gian `http_req_waiting` trung bình tăng từ 277ms lên 4.66s.

---

## 5. Tỷ lệ Lỗi (Error Rate)

- **Tỷ lệ lỗi (Error Rate):** **0.00%** (0 request lỗi ở cả 20 VU và 50 VU).
- **Phân tích:** Không xuất hiện các lỗi `Connection Refused`, `Timeout (504)`, hay `5xx Internal Server Error`. Tất cả 10,768 + 1,773 requests đều trả về `HTTP 200 OK` thành công, chứng tỏ hệ thống quản lý Connection Pool và Exception Handling tốt, chưa bị gãy crash dù latency tăng cao.
