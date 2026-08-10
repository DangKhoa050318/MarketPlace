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
| **http_req_duration p(95)** | **52.84 ms** | **164.53 ms** | **71.45 ms** |
| **http_req_failed rate** | *(Chung)* | *(Chung)* | **91.53 %** |
| **Throughput (req/s)** | - | - | **19.36 req/s** |

## So sánh & Kết luận
- **Đánh giá thời gian phản hồi:** Tốc độ phản hồi trung bình rất nhanh, đạt chuẩn `p(95) < 500ms` như yêu cầu đề ra (thực tế đo được là 71.45ms).
- **So sánh giữa size=10 và size=100:** Khi lấy `size=100` (164.53ms), thời gian phản hồi chậm hơn gấp 3 lần so với `size=10` (52.84ms). Tuy vậy, thời gian lấy 100 sản phẩm vẫn nằm trong ngưỡng cho phép (< 500ms), chứng tỏ chức năng Query/Sort hoạt động khá tốt và Database không mất quá nhiều thời gian để fetch dữ liệu.
- **Kết luận chung:** Dù tốc độ phản hồi cực tốt, hệ thống vẫn **CỰC KỲ KHÔNG ỔN ĐỊNH** dưới mức tải này. Với cấu hình 20 user truy cập đồng thời, tỉ lệ request thất bại lên tới **91.53%** (1081/1181 request bị lỗi). Sự kết hợp giữa tốc độ nhanh + tỉ lệ lỗi cao báo hiệu rằng API đã lập tức văng lỗi Exception (như 404 hoặc 500) và từ chối xử lý request thay vì bị nghẽn (treo hệ thống). Cần team Dev mở log lỗi của Backend lập tức để rà soát nguyên nhân (do phân trang vượt giới hạn hay Pool Connections).
