import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

export const options = {
    vus: 10, // Dùng 10 VU để test thử 10 user đã setup
    duration: '10s',
};

// Hàm setup() chỉ chạy MỘT LẦN duy nhất khi bắt đầu test
export function setup() {
    const users = [];
    const loginUrl = 'http://localhost:8080/api/v1/auth/login';

    // Đăng nhập cho 10 user: demo_customer_01 đến demo_customer_10
    for (let i = 1; i <= 10; i++) {
        // Format số có 2 chữ số (ví dụ: 01, 02)
        const username = `demo_customer_${i.toString().padStart(2, '0')}`;
        const payload = JSON.stringify({
            username: username,
            password: 'admin123',
        });

        const params = {
            headers: {
                'Content-Type': 'application/json',
            },
        };

        const res = http.post(loginUrl, payload, params);

        check(res, {
            [`login ${username} success`]: (r) => r.status === 200,
        });

        if (res.status === 200) {
            const body = res.json();
            if (body.success && body.data && body.data.accessToken) {
                users.push({
                    username: username,
                    token: body.data.accessToken
                });
            }
        }
    }

    // Trả về mảng user để hàm default function() có thể dùng (lưu vào biến 'data')
    return users;
}

// Hàm default function sẽ nhận 'data' do hàm setup() trả về
export default function (users) {
    // Lấy VU ID hiện tại (k6 đếm từ 1)
    const vuId = exec.vu.idInTest;
    
    // Mỗi VU lấy 1 user (vuId - 1 để tương ứng với index của mảng)
    // Lưu ý: nếu số lượng VU > số user, script có thể lỗi out of bound, nên dùng % users.length nếu cần
    const user = users[(vuId - 1) % users.length]; 
    
    // Header chứa token
    const params = {
        headers: {
            'Authorization': `Bearer ${user.token}`,
            'Content-Type': 'application/json'
        }
    };

    // --- Ví dụ request yêu cầu xác thực (thay bằng API thật nếu muốn) ---
    // Gợi ý: Nếu cần lấy thông tin sản phẩm (chỉ ví dụ vì API products không cần token)
    // const res = http.get('http://localhost:8080/api/v1/products', params);
    // check(res, { 'status is 200': (r) => r.status === 200 });

    console.log(`VU ${vuId} đang chạy với user: ${user.username}`);
    
    sleep(1);
}
