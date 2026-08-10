import http from 'k6/http';
import { check, sleep } from 'k6';

const profiles = {
  smoke: {
    vus: 1,
    duration: '10s',
  },
  load: {
    stages: [
      { duration: '10s', target: 5 },
      { duration: '20s', target: 10 },
      { duration: '10s', target: 0 },
    ],
  },
  stress: {
    stages: [
      { duration: '10s', target: 10 },
      { duration: '20s', target: 20 },
      { duration: '10s', target: 0 },
    ],
  },
  // Soak: Test thời gian dài để phát hiện Memory/Connection Leak
  soak: {
    vus: 5,
    duration: '2m',
  },
  // Spike: Tải tăng đột biến để test xem API có bị crash không
  spike: {
    stages: [
      { duration: '5s', target: 2 },
      { duration: '5s', target: 30 },
      { duration: '5s', target: 2 },
    ],
  }
};

const profileName = __ENV.PROFILE || 'smoke';
const selectedProfile = profiles[profileName] || profiles.smoke;

export const options = Object.assign({
  thresholds: {
    http_req_failed: ['rate<0.05'],      // Cho phép lỗi < 5% do phụ thuộc vào db state
    http_req_duration: ['p(95)<2000'],   // 95% request hoàn thành dưới 2 giây
  }
}, selectedProfile);

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
  
  // 1. Đăng nhập để lấy Token
  const loginPayload = JSON.stringify({
    username: 'demo_customer_01',
    password: 'admin123'
  });
  
  const loginRes = http.post(`${baseUrl}/api/v1/auth/login`, loginPayload, {
    headers: { 'Content-Type': 'application/json' },
  });

  let token = '';
  if (check(loginRes, { 'Login status is 200': (r) => r.status === 200 })) {
    try {
      const body = JSON.parse(loginRes.body);
      token = body.data.accessToken;
    } catch (e) {}
  }

  if (!token) {
    // Nếu không lấy được token thì skip luồng sau
    sleep(1);
    return;
  }

  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };

  // 2. Thêm sản phẩm vào giỏ hàng
  // Sử dụng variantId = 1 hoặc 2 làm mẫu. (Thực tế nên random hoặc lấy từ catalog)
  const cartPayload = JSON.stringify({
    variantId: 1,
    quantity: 1
  });

  const cartRes = http.post(`${baseUrl}/api/v1/cart/items`, cartPayload, { headers: authHeaders });
  
  check(cartRes, {
    'Add to cart status is 200 or 201': (r) => r.status === 200 || r.status === 201,
  });

  sleep(1);

  // 3. Tạo đơn hàng (Checkout)
  const orderPayload = JSON.stringify({
    shippingAddress: '123 Test Street, HCM City',
    note: 'Load testing order',
    paymentMethod: 'COD'
  });

  const orderRes = http.post(`${baseUrl}/api/v1/orders`, orderPayload, { headers: authHeaders });
  
  check(orderRes, {
    'Create order status is 201 (Created) or 200': (r) => r.status === 201 || r.status === 200,
  });

  sleep(2);
}
