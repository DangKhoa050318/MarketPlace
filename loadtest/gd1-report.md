# Báo cáo Load Test - API Đọc Danh Sách Sản Phẩm (/products)

- **Người thực hiện:** Bùi Yến Nhi
- **Ngày thực hiện:** 10/08/2026
- **Môi trường:** Localhost (http://localhost:8080)
- **Database:** PostgreSQL (HikariCP maximum-pool-size = 20)

## Cấu hình kịch bản
- **Số lượng VU (Virtual Users):** 20
- **Thời gian chạy:** 1 phút
- **Kịch bản:** Gửi GET request tới `/api/v1/products` với các tham số phân trang. Tham số `page` được random từ `0` đến `9`. Tham số `size` được random chọn giữa `10` hoặc `100`.

## Bảng Số Liệu Kết Quả (Sau khi tắt Rate Limit và giả lập 20 IP)

| Chỉ số (Metrics) | Kịch bản `size=10` | Kịch bản `size=100` | Tổng quan (Tất cả) |
| :--- | :--- | :--- | :--- |
| **http_req_duration p(95)** | **24.41 ms** | **23.36 ms** | **24.24 ms** |
| **http_req_failed rate** | *(Chung)* | *(Chung)* | **0.00 %** |
| **Throughput (req/s)** | - | - | **19.70 req/s** |

## So sánh & Kết luận
- **Sự cố 91.53% lỗi ở lần test trước:** Nguyên nhân gây ra lỗi hàng loạt ở lần test trước đã được xác định là do 20 Virtual Users chia sẻ chung 1 IP, dẫn đến việc bị Filter `RateLimitingFilter.java` của Redis chặn (giới hạn 100 req/phút). Sau khi **tạm tắt Rate Limit** để benchmark thuần túy DB và cấu hình mock IP (`X-Forwarded-For`), tỉ lệ lỗi đã về mức hoàn hảo **0%**.
- **Đánh giá thời gian phản hồi (Latency):** Tốc độ phản hồi cực kỳ xuất sắc. 95% số request hoàn thành dưới **24.24ms**, vượt xa KPI (< 500ms). Điều này chứng minh hệ thống xử lý logic và truy vấn siêu nhẹ.
- **So sánh phân trang size=10 và size=100:** Thời gian query 100 sản phẩm (23.36ms) và 10 sản phẩm (24.41ms) là **ngang ngửa nhau**. DB (Postgres) fetch data rất tối ưu nhờ Index, việc bóc tách số lượng lớn records không tạo ra overhead đáng kể.
- **Sức khỏe Connection Pool:** Không hề có hiện tượng chờ đợi cấp phát connection (Starvation). Các thread kết nối xuống DB hoạt động trơn tru. 
- **Đề xuất bước tiếp theo:** Ở mức tải 20 req/s, hệ thống vẫn "chưa xi nhê". Để dò đúng giới hạn đỏ của DB và Pool, đề xuất tăng `VUs` lên 100 và gỡ bỏ `sleep(1)` trong script K6 cho đợt stress test tiếp theo.
