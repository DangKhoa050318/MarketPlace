import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

const profiles = {
  smoke: { vus: 1, duration: '10s' },
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
  soak: { vus: 5, duration: '2m' },
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
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<2000'],
  }
}, selectedProfile);

// Hàm setup() chạy 1 lần duy nhất trước khi bắt đầu VUs
export function setup() {
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
  const users = [];

  // Login cho 20 user khác nhau để test đa luồng
  // Lưu ý: Dùng X-Forwarded-For để spoof IP, tránh bị RateLimitingFilter chặn (limit 5 req/min/IP)
  for (let i = 1; i <= 20; i++) {
    const userStr = i < 10 ? `0${i}` : `${i}`;
    const username = `demo_customer_${userStr}`;
    const loginPayload = JSON.stringify({
      username: username,
      password: 'admin123'
    });
    
    const randomIp = `192.168.1.${i}`;
    const loginRes = http.post(`${baseUrl}/api/v1/auth/login`, loginPayload, {
      headers: { 
        'Content-Type': 'application/json',
        'X-Forwarded-For': randomIp
      },
    });

    if (loginRes.status === 200) {
      try {
        const body = JSON.parse(loginRes.body);
        users.push({
          username: username,
          token: body.data.accessToken
        });
      } catch (e) {}
    }
  }

  // Lấy danh sách sản phẩm/variant hợp lệ từ API thay vì hardcode
  const catalogRes = http.get(`${baseUrl}/api/v1/products/catalog?page=0&size=50`);
  let variantIds = [1, 2, 3]; // fallback
  if (catalogRes.status === 200) {
    try {
      const body = JSON.parse(catalogRes.body);
      if (body.data && body.data.content && body.data.content.length > 0) {
        variantIds = [];
        body.data.content.forEach(product => {
          if (product.variants && product.variants.length > 0) {
            product.variants.forEach(v => variantIds.push(v.id));
          }
        });
      }
    } catch (e) {}
  }

  // Truyền data này xuống cho mọi VU
  return { users, variantIds };
}

export default function (data) {
  if (!data || !data.users || data.users.length === 0) {
    sleep(1);
    return;
  }
  
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

  // Chia đều user cho các VU
  const vuId = exec.vu.idInTest;
  const userIndex = (vuId - 1) % data.users.length;
  const user = data.users[userIndex];
  const token = user.token;

  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };

  // Chọn random 1 variant từ danh sách thực tế
  const variantId = data.variantIds[Math.floor(Math.random() * data.variantIds.length)];

  // Thêm sản phẩm vào giỏ hàng
  const cartPayload = JSON.stringify({
    variantId: variantId,
    quantity: 1
  });

  const cartRes = http.post(`${baseUrl}/api/v1/cart/items`, cartPayload, { headers: authHeaders });
  
  check(cartRes, {
    'Add to cart status is 200 or 201': (r) => r.status === 200 || r.status === 201,
  });

  sleep(1);

  // Tạo đơn hàng (Checkout)
  const orderPayload = JSON.stringify({
    shippingAddress: '123 Test Street, HCM City',
    note: 'Load testing order by ' + user.username,
    paymentMethod: 'COD'
  });

  const orderRes = http.post(`${baseUrl}/api/v1/orders`, orderPayload, { headers: authHeaders });
  
  check(orderRes, {
    'Create order status is 201 (Created) or 200': (r) => r.status === 201 || r.status === 200,
  });

  sleep(1);
}

// Hàm teardown() chạy 1 lần duy nhất sau khi test xong
export function teardown(data) {
  if (!data || !data.users) return;
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

  // Xóa giỏ hàng của tất cả user đã tham gia test
  for (const user of data.users) {
    http.del(`${baseUrl}/api/v1/cart`, null, {
      headers: {
        'Authorization': `Bearer ${user.token}`
      }
    });
  }
}
