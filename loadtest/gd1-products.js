import http from 'k6/http';
import { check, sleep } from 'k6';
import { vu } from 'k6/execution';

export const options = {
    vus: 20, // 20 Virtual Users
    duration: '1m', // Chạy trong 1 phút
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95% request phải hoàn thành dưới 500ms
        'http_req_duration{size:10}': ['p(95)<500'], // Để k6 in ra kết quả riêng cho size=10
        'http_req_duration{size:100}': ['p(95)<500'], // Để k6 in ra kết quả riêng cho size=100
        http_req_failed: ['rate<0.01'], // Tỉ lệ lỗi phải dưới 1%
    },
};

export default function () {
    // Random page từ 0 đến 9
    const page = Math.floor(Math.random() * 10);
    
    // Random size là 10 hoặc 100
    const size = Math.random() < 0.5 ? 10 : 100;
    
    // Tạo URL với tham số truyền vào
    const url = `http://localhost:8080/api/v1/products?page=${page}&size=${size}&sortBy=createdAt&sortDir=DESC`;
    
    // Tạo IP giả lập dựa trên ID của mỗi Virtual User (1-20)
    const mockIp = `192.168.1.${vu.idInTest}`;
    
    // Gắn tag 'size' để dễ dàng lọc/phân tích số liệu riêng cho từng kịch bản, cùng với Header IP
    const params = {
        headers: { 'X-Forwarded-For': mockIp },
        tags: { size: size.toString() },
    };

    // Gửi GET request
    const res = http.get(url, params);

    // Kiểm tra HTTP 200 và success === true trong response JSON
    check(res, {
        'status is 200': (r) => r.status === 200,
        'success is true': (r) => {
            try {
                const body = r.json();
                return body.success === true;
            } catch (e) {
                return false;
            }
        }
    });

    // Nghỉ 1 giây giữa mỗi lần request
    sleep(1);
}
