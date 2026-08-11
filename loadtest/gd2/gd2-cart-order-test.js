import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

// Ramp profile GĐ2: 20s ramp-up 10 VUs -> 1m duy trì 10 VUs -> 20s ramp-down 0 VUs
export const options = {
  stages: [
    { duration: '20s', target: 10 },
    { duration: '1m', target: 10 },
    { duration: '20s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],               // Tỷ lệ lỗi < 5%
    http_req_duration: ['p(95)<3000'],             // Response time p(95) < 3000ms cho luồng ghi DB
    'http_req_duration{name:add_to_cart}': ['p(95)<2000'],
    'http_req_duration{name:get_cart}': ['p(95)<1000'],
    'http_req_duration{name:create_order}': ['p(95)<3000'],
  },
};

// Hàm setup() chạy 1 lần duy nhất trước khi bắt đầu VUs
export function setup() {
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
  const users = [];

  // Login 10 users từ demo_customer_01 đến demo_customer_10
  const testPassword = __ENV.TEST_PASSWORD || 'admin123';
  for (let i = 1; i <= 10; i++) {
    const userStr = i < 10 ? `0${i}` : `${i}`;
    const username = `demo_customer_${userStr}`;
    const loginPayload = JSON.stringify({
      username: username,
      password: testPassword
    });
    
    const loginRes = http.post(`${baseUrl}/api/v1/auth/login`, loginPayload, {
      headers: { 
        'Content-Type': 'application/json',
        'X-Bypass-Rate-Limit': 'true'
      },
    });

    if (loginRes.status === 200) {
      try {
        const body = JSON.parse(loginRes.body);
        users.push({
          username: username,
          token: body.data.accessToken
        });
      } catch (e) {
        console.error(`Failed to parse login response for ${username}:`, e);
      }
    } else {
      console.error(`Failed to login ${username}, status: ${loginRes.status}`);
    }
  }

  // Lấy danh sách sản phẩm/variant hợp lệ từ API catalog
  const catalogRes = http.get(`${baseUrl}/api/v1/products/catalog?page=0&size=50`, {
    headers: { 'X-Bypass-Rate-Limit': 'true' }
  });
  
  let variantIds = [];
  if (catalogRes.status === 200) {
    try {
      const body = JSON.parse(catalogRes.body);
      if (body.data && body.data.content && body.data.content.length > 0) {
        body.data.content.forEach(product => {
          if (product.variants && product.variants.length > 0) {
            product.variants.forEach(v => variantIds.push(v.id));
          }
        });
      }
    } catch (e) {
      console.error('Failed to parse catalog response:', e);
    }
  }

  if (variantIds.length === 0) {
    console.error('CRITICAL: No variant IDs found in catalog! Using fallback IDs.');
    variantIds = [1, 2, 3];
  }

  return { users, variantIds };
}

export default function (data) {
  if (!data || !data.users || data.users.length === 0) {
    sleep(1);
    return;
  }
  
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

  // Đảm bảo 1 VU = 1 user riêng biệt (map VU index -> user token theo modulo)
  const vuId = exec.vu.idInTest;
  const userIndex = (vuId - 1) % data.users.length;
  const user = data.users[userIndex];
  const token = user.token;

  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
    'X-Bypass-Rate-Limit': 'true'
  };

  // Chọn random 1 variant hợp lệ
  const variantId = data.variantIds[Math.floor(Math.random() * data.variantIds.length)];

  // Bước 1: Thêm sản phẩm vào giỏ hàng (POST /api/v1/cart/items)
  const cartPayload = JSON.stringify({
    variantId: variantId,
    quantity: 1
  });

  const cartRes = http.post(`${baseUrl}/api/v1/cart/items`, cartPayload, {
    headers: authHeaders,
    tags: { name: 'add_to_cart' }
  });
  
  check(cartRes, {
    'POST /cart/items status is 200 or 201': (r) => r.status === 200 || r.status === 201,
  });

  sleep(0.5);

  // Bước 2: Xem giỏ hàng (GET /api/v1/cart)
  const getCartRes = http.get(`${baseUrl}/api/v1/cart`, {
    headers: authHeaders,
    tags: { name: 'get_cart' }
  });

  check(getCartRes, {
    'GET /cart status is 200': (r) => r.status === 200,
  });

  sleep(0.5);

  // Bước 3: Đặt hàng (POST /api/v1/orders)
  const orderPayload = JSON.stringify({
    shippingAddress: '123 LoadTest Street, District 1, HCM City',
    note: `Load test order by VU ${vuId} (${user.username})`,
    paymentMethod: 'COD'
  });

  const orderRes = http.post(`${baseUrl}/api/v1/orders`, orderPayload, {
    headers: authHeaders,
    tags: { name: 'create_order' }
  });
  
  check(orderRes, {
    'POST /orders status is 201 or 200': (r) => r.status === 201 || r.status === 200,
  });

  sleep(1);
}

// Hàm teardown() dọn dẹp giỏ hàng
export function teardown(data) {
  if (!data || !data.users) return;
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

  for (const user of data.users) {
    http.del(`${baseUrl}/api/v1/cart`, null, {
      headers: {
        'Authorization': `Bearer ${user.token}`,
        'X-Bypass-Rate-Limit': 'true'
      }
    });
  }
}
