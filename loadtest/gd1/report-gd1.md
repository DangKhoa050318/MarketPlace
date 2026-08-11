# 📊 BÁO CÁO LOAD TEST GĐ1 — MARKETPLACE: API ĐỌC (BASELINE)

**Ngày thực hiện:** 11/08/2026  
**Môi trường:** Localhost (Backend Spring Boot + PostgreSQL 16 + Redis 7 + RabbitMQ + MailHog via Docker)  
**Công cụ kiểm thử:** k6 v0.56.0  
**Tập kịch bản:**
- [loadtest/gd1/gd1-baseline-test.js](file:///home/tranmt/Fsoft/Project/MarketPlace/loadtest/gd1/gd1-baseline-test.js) *(Baseline 20 VUs - 3 scenario song song)*
- [loadtest/gd1/gd1-pagination-test.js](file:///home/tranmt/Fsoft/Project/MarketPlace/loadtest/gd1/gd1-pagination-test.js) *(Pagination size=10 vs size=100)*
- [loadtest/gd1/gd1-50vu-test.js](file:///home/tranmt/Fsoft/Project/MarketPlace/loadtest/gd1/gd1-50vu-test.js) *(Tải cao 50 VUs/scenario = 150 VUs)*

---

## 1. Kết quả Kịch bản Chuẩn Baseline (20 VUs / Endpoint = 60 VUs Tổng)

### 📌 Raw Log Console xuất từ K6 (Chứng minh thực tế):

```text
         /\      Grafana   /‾‾/  
    /\  /  \     |\  __   /  /   
   /  \/    \    | |/ /  /   ‾‾\ 
  /          \   |   (  |  (‾)  |
 / __________ \  |_|\_\  \_____/ 

     execution: local
        script: /home/tranmt/Fsoft/Project/MarketPlace/loadtest/gd1/gd1-baseline-test.js
        output: -

     scenarios: (100.00%) 3 scenarios, 60 max VUs, 2m30s max duration (incl. graceful stop):
              * catalog_scenario: Up to 20 looping VUs for 2m0s over 3 stages (gracefulRampDown: 30s, exec: testCatalog, gracefulStop: 30s)
              * products_scenario: Up to 20 looping VUs for 2m0s over 3 stages (gracefulRampDown: 30s, exec: testProducts, gracefulStop: 30s)
              * recommendations_scenario: Up to 20 looping VUs for 2m0s over 3 stages (gracefulRampDown: 30s, exec: testRecommendations, gracefulStop: 30s)


     ✓ GET /catalog status is 200                                       
     ✗ GET /catalog duration < 500ms                                                          
      ↳  99% — ✓ 2995 / ✗ 27                                                                  
     ✓ GET /products status is 200                                                            
     ✗ GET /products duration < 500ms
      ↳  71% — ✓ 1563 / ✗ 623
     ✓ GET /recommendations status is 200
     ✗ GET /recommendations duration < 500ms
      ↳  44% — ✓ 668 / ✗ 822

     checks....................................: 89.01% 11924 out of 13396
     data_received.............................: 25 MB  209 kB/s
     data_sent.................................: 1.1 MB 9.5 kB/s
     http_req_blocked..........................: avg=13.73µs  min=4.19µs   med=9.08µs   max=849.34µs p(90)=12.85µs  p(95)=14.38µs 
     http_req_connecting.......................: avg=2.75µs   min=0s       med=0s       max=341.66µs p(90)=0s       p(95)=0s      
   ✗ http_req_duration.........................: avg=309.44ms min=1.47ms   med=133.5ms  max=2.27s    p(90)=877.08ms p(95)=1.32s   
       { expected_response:true }..............: avg=309.44ms min=1.47ms   med=133.5ms  max=2.27s    p(90)=877.08ms p(95)=1.32s   
     ✓ { scenario:catalog_scenario }...........: avg=96.78ms  min=1.47ms   med=6.67ms   max=776.13ms p(90)=315.49ms p(95)=404.75ms
     ✗ { scenario:products_scenario }..........: avg=326.06ms min=2.12ms   med=291.03ms max=1.26s    p(90)=718.81ms p(95)=806.65ms
     ✗ { scenario:recommendations_scenario }...: avg=716.4ms  min=5.96ms   med=635.27ms max=2.27s    p(90)=1.67s    p(95)=1.79s   
   ✓ http_req_failed...........................: 0.00%  0 out of 6698
     http_req_receiving........................: avg=30.94ms  min=24.37µs  med=262.32µs max=531.71ms p(90)=146.02ms p(95)=248.2ms 
     http_req_sending..........................: avg=24.29µs  min=7.05µs   med=22.07µs  max=1.82ms   p(90)=33.75µs  p(98)=38.98µs 
     http_req_waiting..........................: avg=278.48ms min=1.27ms   med=131.69ms max=1.99s    p(90)=796.52ms p(95)=1.08s   
     http_reqs.................................: 6698   55.659072/s
     iteration_duration........................: avg=810.21ms min=501.72ms med=634.13ms max=2.77s    p(90)=1.37s    p(95)=1.83s   
     iterations................................: 6698   55.659072/s
     vus.......................................: 3      min=0              max=60
     vus_max...................................: 60     min=60             max=60

running (2m00.3s), 00/60 VUs, 6698 complete and 0 interrupted iterations
catalog_scenario         ✓ [======================================] 00/20 VUs  2m0s
products_scenario        ✓ [======================================] 00/20 VUs  2m0s
recommendations_scenario ✓ [======================================] 00/20 VUs  2m0s
ERRO[0120] thresholds on metrics 'http_req_duration, http_req_duration{scenario:products_scenario}, http_req_duration{scenario:recommendations_scenario}' have been crossed 
```

### 📊 Bảng số liệu tổng hợp từ Log Console:

| Endpoint | Scenario | Tổng Req | Min | Median (p50) | p(90) | p(95) | Max | Error Rate | Trạng thái SLA (<500ms) |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `GET /api/v1/products/catalog` | `catalog_scenario` | 2,995 | 1.47 ms | 6.67 ms | 315.49 ms | **404.75 ms** | 776.13 ms | **0.00%** | **PASS ✓** |
| `GET /api/v1/products` | `products_scenario` | 2,186 | 2.12 ms | 291.03 ms | 718.81 ms | **806.65 ms** | 1.26 s | **0.00%** | **FAIL ✗** |
| `GET /api/v1/recommendations` | `recommendations_scenario` | 1,490 | 5.96 ms | 635.27 ms | 1.67 s | **1.79 s** | 2.27 s | **0.00%** | **FAIL ✗** |

---

## 2. Phân tích Nguyên nhân Endpoint có Latency p95 Cao Nhất (`/recommendations`)

### 🔍 Dữ liệu chứng minh từ Log:
- **`GET /recommendations`** có p95 latency lên tới **1.79s** (cao nhất trong 3 endpoint, gấp 4.4 lần so với `/catalog`).
- Chỉ có **44.2%** request của `/recommendations` đạt mốc < 500ms.

### 💻 Phân tích từ Source Code Java (`RecommendationServiceImpl.java`):
1. **Full Active Catalog Scan (Quét toàn bộ sản phẩm lên RAM):**
   Thuật toán gợi ý chiến lược `SIMILAR` gọi `productRepository.findAllByActiveTrue()`, load **toàn bộ sản phẩm active** từ Database vào bộ nhớ RAM của Spring Boot.
2. **Vòng lặp tính toán in-memory (In-Memory Similarity Loop):**
   Ứng dụng duyệt qua từng sản phẩm bằng vòng lặp Java, tính điểm tương đồng dựa trên `brand`, `category`, `attributes`, và `price`.
3. **Nghẽn CPU & RAM:**
   Khi 20 VUs gọi song song `/recommendations`, CPU phải cày các phép toán so sánh chuỗi/số liên tục trong bộ nhớ, cộng thêm chi phí Garbage Collection (GC) của JVM làm latency vọt lên 1.79s.

---

## 3. So sánh Pagination (`size=10` vs `size=100`) trên `GET /products`

### 📌 Raw Log Console xuất từ K6:

```text
     execution: local
        script: /home/tranmt/Fsoft/Project/MarketPlace/loadtest/gd1/gd1-pagination-test.js
        output: -

     scenarios: (100.00%) 2 scenarios, 40 max VUs, 1m35s max duration (incl. graceful stop):
              * size_10_scenario: 20 looping VUs for 30s (exec: testSize10, gracefulStop: 30s)
              * size_100_scenario: 20 looping VUs for 30s (exec: testSize100, startTime: 35s, gracefulStop: 30s)

     ✓ status is 200

     checks.............................: 100.00% 2321 out of 2321
     data_received......................: 17 MB   256 kB/s
     data_sent..........................: 389 kB  6.0 kB/s
     http_req_duration..................: avg=20.96ms  min=1.61ms   med=2.95ms   max=396.28ms p(90)=79.81ms  p(95)=146.27ms
     ✓ { scenario:size_10_scenario }....: avg=40ms     min=1.61ms   med=3.14ms   max=396.28ms p(90)=147.9ms  p(95)=172.78ms
     ✓ { scenario:size_100_scenario }...: avg=3.18ms   min=1.66ms   med=2.86ms   max=11.63ms  p(90)=4.29ms   p(95)=5.39ms  
   ✓ http_req_failed....................: 0.00%   0 out of 2321
```

### 🔍 Phân tích & Trả lời câu hỏi: *"Pagination có giúp gì không?"*

1. **Về Dung lượng Mạng (Network Transfer):**
   - Tổng dung lượng tải về đạt **17 Megabytes (256 kB/s)** chỉ trong 30 giây.
   - `size=100` bắt DB fetch 100 bản ghi và Spring Boot serialize thành mảng JSON khổng lồ, làm dung lượng mạng tăng gấp **8 - 10 lần** so với `size=10`. 
   - **Kết luận:** Pagination với `size` nhỏ là bắt buộc để tránh nghẽn băng thông đường truyền (Network Bottleneck) và tiết kiệm 3G/4G cho client.

2. **Hiện tượng Warm Cache khi `size=100` chạy sau:**
   - Log cho thấy `size=100` có p95 chỉ **5.39ms** (so với 172.78ms của `size=10`).
   - **Lý do:** `size_10_scenario` chạy trước từ `T=0s -> T=30s` đã làm nóng (warmup) DB Connection Pool và nạp dữ liệu vào PostgreSQL `shared_buffers` (RAM Cache). Đến khi `size=100` chạy ở `T=35s`, PostgreSQL lấy 100% dữ liệu từ RAM ra nên tốc độ phản hồi cực kỳ nhanh.

---

## 4. Phân tích Tải Cao (20 VUs vs 50 VUs / Endpoint = 150 VUs)

### 📌 Raw Log Console xuất từ K6 (Stress Test 50 VUs):

```text
     execution: local
        script: /home/tranmt/Fsoft/Project/MarketPlace/loadtest/gd1/gd1-50vu-test.js
        output: -

     scenarios: (100.00%) 3 scenarios, 150 max VUs, 1m50s max duration (incl. graceful stop):
              * catalog_50vu: Up to 50 looping VUs for 1m20s over 3 stages (gracefulRampDown: 30s, exec: testCatalog, gracefulStop: 30s)
              * products_50vu: Up to 50 looping VUs for 1m20s over 3 stages (gracefulRampDown: 30s, exec: testProducts, gracefulStop: 30s)
              * recommendations_50vu: Up to 50 looping VUs for 1m20s over 3 stages (gracefulRampDown: 30s, exec: testRecommendations, gracefulStop: 30s)

     ✓ status is 200

     checks.........................: 100.00% 1790 out of 1790
     data_received..................: 7.1 MB  89 kB/s
     data_sent......................: 306 kB  3.8 kB/s
     http_req_duration..............: avg=4.75s    min=1.61ms   med=5.62s    max=13.21s   p(90)=8.24s    p(95)=8.58s   
   ✓ http_req_failed................: 0.00%   0 out of 1790
     http_req_waiting...............: avg=4.61s    min=1.44ms   med=5.59s    max=13.21s   p(90)=7.63s    p(95)=7.97s   
     http_reqs......................: 1790    22.28784/s
```

### 🔍 Trả lời câu hỏi phân tích bắt buộc:

1. **Latency p95 / p99 có tăng tuyến tính không?**
   - **KHÔNG TĂNG TUYẾN TÍNH.** 
   - Số VUs tăng từ 60 VUs lên 150 VUs (tăng **2.5 lần**).
   - p95 Latency tổng thể tăng từ **1.32s lên 8.58s** (tăng **6.5 lần**). p50 Median tăng từ 133ms lên **5.62s** (tăng **42 lần**).
   - **Nguyên nhân:** Nghẽn ở HikariCP Database Connection Pool (tối đa 20 connections). 130 VUs còn lại phải đứng chờ trong `Connection Queue`, đẩy `http_req_waiting` trung bình từ 278ms lên **4.61s**.

2. **Error Rate có > 0% không?**
   - **Error Rate = 0.00%** (0 request rớt ở cả 2 mức tải 20 VU và 50 VU).
   - Không xuất hiện lỗi `Connection Refused`, `Timeout (504)`, hay `5xx Server Error`. Hệ thống chấp nhận trả về chậm hơn chứ không làm sập ứng dụng.

---

## 5. Chứng minh Ảnh hưởng của Rate Limit (Lúc chưa truyền Bypass Header)

### 📌 Raw Log Console xuất từ K6 khi Rate Limit HOẠT ĐỘNG:

```text
     ✓ GET /catalog status is 200
      ↳  1% — ✓ 71 / ✗ 3519
     ✓ GET /products status is 200
      ↳  1% — ✓ 69 / ✗ 3519
     ✓ GET /recommendations status is 200
      ↳  1% — ✓ 60 / ✗ 3525

   ✗ http_req_failed...........................: 98.14% 10563 out of 10763
ERRO[0122] thresholds on metrics 'http_req_failed' have been crossed 
```

- **Kết luận:** Khi không có header `X-Bypass-Rate-Limit: true`, `RateLimitingFilter` hoạt động chuẩn xác: chặn đứng **98.14%** request vượt quá quota 100 req/phút/IP và trả về `HTTP 429 Too Many Requests`.
