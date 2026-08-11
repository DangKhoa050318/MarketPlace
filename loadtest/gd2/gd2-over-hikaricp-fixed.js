import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

// GĐ2 stress: 30 VUs, mỗi VU = 1 user riêng, 1 iteration (add_to_cart -> create_order).
// Đo khả năng vượt hikari.maximum-pool-size=20 thật sự, tránh lỗi "cart empty" do share user.
export const options = {
  scenarios: {
    gd2_stress: {
      executor: 'per-vu-iterations',
      vus: 30,
      iterations: 1,
      maxDuration: '2m',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.10'],
  },
};

export function setup() {
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8081';
  const users = [];
  const testPassword = __ENV.TEST_PASSWORD || 'admin123';
  for (let i = 1; i <= 30; i++) {
    const userStr = i < 10 ? `0${i}` : `${i}`;
    const username = `demo_customer_${userStr}`;
    const loginRes = http.post(`${baseUrl}/api/v1/auth/login`, JSON.stringify({ username, password: testPassword }), {
      headers: { 'Content-Type': 'application/json', 'X-Bypass-Rate-Limit': 'true' },
    });
    if (loginRes.status === 200) {
      const body = JSON.parse(loginRes.body);
      users.push({ username, token: body.data.accessToken });
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

  http.post(`${baseUrl}/api/v1/cart/items`, JSON.stringify({ variantId, quantity: 1 }), { headers: authHeaders, tags: { name: 'add_to_cart' } });

  const orderRes = http.post(`${baseUrl}/api/v1/orders`, JSON.stringify({ shippingAddress: 'Over Hikari Test', paymentMethod: 'COD' }), { headers: authHeaders, tags: { name: 'create_order' } });
  check(orderRes, { 'Order ok': (r) => r.status === 201 || r.status === 200 });
}
