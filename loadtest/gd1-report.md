# Báo cáo Load Test - API Đọc Danh Sách Sản Phẩm (/products)

- **Người thực hiện:** Bùi Yến Nhi
- **Ngày thực hiện:** 10/08/2026
- **Môi trường:** Localhost (http://localhost:8080)
- **Database:** PostgreSQL (HikariCP maximum-pool-size = 20)

## Cấu hình kịch bản
- **Số lượng VU (Virtual Users):** 20
- **Thời gian chạy:** 1 phút
- **Kịch bản:** Gửi GET request tới `/api/v1/products` với các tham số phân trang. Tham số `page` được random từ `0` đến `9`. Tham số `size` được random chọn giữa `10` hoặc `100`.

## Bảng Số Liệu Kết Quả

| Chỉ số (Metrics) | Kịch bản `size=10` | Kịch bản `size=100` | Tổng quan (Tất cả) |
| :--- | :--- | :--- | :--- |
| **http_req_duration p(95)** | *(Chạy lại kịch bản để có số liệu)* | *(Chạy lại kịch bản để có số liệu)* | **49.25 ms** |
| **http_req_failed rate** | *(Chạy lại kịch bản để có số liệu)* | *(Chạy lại kịch bản để có số liệu)* | **91.52 %** |
| **Throughput (req/s)** | - | - | **19.61 req/s** |

## So sánh & Kết luận
- **Đánh giá thời gian phản hồi:** Tốc độ phản hồi trung bình rất nhanh, đạt chuẩn `p(95) < 500ms` như yêu cầu đề ra (thực tế đo được là 49.25ms).
- **So sánh giữa size=10 và size=100:** (Bạn hãy chạy lại lệnh `k6 run loadtest/gd1-products.js` với code mới để xem chỉ số riêng biệt và điền vào đây).
- **Kết luận chung:** Hệ thống **CỰC KỲ KHÔNG ỔN ĐỊNH** dưới tải. Với cấu hình 20 user truy cập đồng thời, tỉ lệ request thất bại lên tới **91.52%** (1080/1180 request bị lỗi). Nguyên nhân có thể do cấu hình PostgreSQL `maximum-pool-size = 20` bị nghẽn (connection timeout) hoặc code xử lý phân trang/sort đang gặp lỗi (throw Exception 500) khi bị gọi liên tục. Yêu cầu team Dev mở log Backend để kiểm tra ngay lập tức.
