import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

// GĐ2 fixed: mỗi VU = 1 user riêng (demo_customer_01..10), mỗi VU chạy đúng 1 iteration
// (add_to_cart -> get_cart -> create_order) để tránh race "cart is empty" khi cùng user
// nhiều luồng ghi đè cart. Phản ánh đúng 1 user mua 1 giỏ.
export const options = {
  scenarios: {
    gd2: {
      executor: 'per-vu-iterations',
      vus: 10,
      iterations: 1,
      maxDuration: '2m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
    http_req_duration: ['p(95)<3000'],
    'http_req_duration{name:add_to_cart}': ['p(95)<2000'],
    'http_req_duration{name:get_cart}': ['p(95)<1000'],
    'http_req_duration{name:create_order}': ['p(95)<3000'],
  },
};

export function setup() {
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8081';
  const users = [];
  const testPassword = __ENV.TEST_PASSWORD || 'admin123';
  for (let i = 1; i <= 10; i++) {
    const userStr = i < 10 ? `0${i}` : `${i}`;
    const username = `demo_customer_${userStr}`;
    const loginRes = http.post(`${baseUrl}/api/v1/auth/login`, JSON.stringify({ username, password: testPassword }), {
      headers: { 'Content-Type': 'application/json', 'X-Bypass-Rate-Limit': 'true' },
    });
    if (loginRes.status === 200) {
      const body = JSON.parse(loginRes.body);
      users.push({ username, token: body.data.accessToken });
    } else {
      console.error(`Failed to login ${username}, status: ${loginRes.status}`);
    }
  }

  const catalogRes = http.get(`${baseUrl}/api/v1/products/catalog?page=0&size=50`, {
    headers: { 'X-Bypass-Rate-Limit': 'true' },
  });
  const variantIds = [];
  if (catalogRes.status === 200) {
    const body = JSON.parse(catalogRes.body);
    body.data.content.forEach(p => p.variants && p.variants.forEach(v => variantIds.push(v.id)));
  }
  if (variantIds.length === 0) variantIds.push(1, 2, 3);

  return { users, variantIds };
}

export default function (data) {
  if (!data.users || data.users.length === 0) { sleep(1); return; }
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8081';
  const vuId = exec.vu.idInTest;
  const user = data.users[(vuId - 1) % data.users.length];
  const authHeaders = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${user.token}`, 'X-Bypass-Rate-Limit': 'true' };

  const variantId = data.variantIds[Math.floor(Math.random() * data.variantIds.length)];

  const cartRes = http.post(`${baseUrl}/api/v1/cart/items`, JSON.stringify({ variantId, quantity: 1 }), { headers: authHeaders, tags: { name: 'add_to_cart' } });
  check(cartRes, { 'POST /cart/items status is 200 or 201': (r) => r.status === 200 || r.status === 201 });
  sleep(0.3);

  const getCartRes = http.get(`${baseUrl}/api/v1/cart`, { headers: authHeaders, tags: { name: 'get_cart' } });
  check(getCartRes, { 'GET /cart status is 200': (r) => r.status === 200 });
  sleep(0.3);

  const orderRes = http.post(`${baseUrl}/api/v1/orders`, JSON.stringify({ shippingAddress: '123 LoadTest Street, HCM', note: `Load test by ${user.username}`, paymentMethod: 'COD' }), { headers: authHeaders, tags: { name: 'create_order' } });
  check(orderRes, { 'POST /orders status is 201 or 200': (r) => r.status === 201 || r.status === 200 });
}
