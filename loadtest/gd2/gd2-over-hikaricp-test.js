import http from 'k6/http';
import { check, sleep } from 'k6';
import exec from 'k6/execution';

// Kịch bản đẩy VU lên 30 VUs (vượt quá hikari.maximum-pool-size = 20)
export const options = {
  stages: [
    { duration: '15s', target: 30 },
    { duration: '30s', target: 30 },
    { duration: '15s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.10'],
  },
};

export function setup() {
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
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
    }
  }

  const catalogRes = http.get(`${baseUrl}/api/v1/products/catalog?page=0&size=50`, {
    headers: { 'X-Bypass-Rate-Limit': 'true' }
  });
  let variantIds = [1, 2, 3];
  if (catalogRes.status === 200) {
    try {
      const body = JSON.parse(catalogRes.body);
      if (body.data && body.data.content) {
        variantIds = [];
        body.data.content.forEach(p => p.variants && p.variants.forEach(v => variantIds.push(v.id)));
      }
    } catch (e) {}
  }

  return { users, variantIds };
}

export default function (data) {
  if (!data || !data.users || data.users.length === 0) return;
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';

  const vuId = exec.vu.idInTest;
  const user = data.users[(vuId - 1) % data.users.length];
  const authHeaders = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${user.token}`,
    'X-Bypass-Rate-Limit': 'true'
  };

  const variantId = data.variantIds[Math.floor(Math.random() * data.variantIds.length)];

  // 1. Add to cart
  const cartRes = http.post(`${baseUrl}/api/v1/cart/items`, JSON.stringify({ variantId, quantity: 1 }), {
    headers: authHeaders,
    tags: { name: 'add_to_cart' }
  });
  check(cartRes, { 'Add cart ok': (r) => r.status === 200 || r.status === 201 });
  sleep(0.2);

  // 2. Create order
  const orderRes = http.post(`${baseUrl}/api/v1/orders`, JSON.stringify({
    shippingAddress: 'Over Hikari Test Address',
    paymentMethod: 'COD'
  }), {
    headers: authHeaders,
    tags: { name: 'create_order' }
  });
  check(orderRes, { 'Order ok': (r) => r.status === 201 || r.status === 200 });
  sleep(0.5);
}

export function teardown(data) {
  if (!data || !data.users) return;
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
  for (const user of data.users) {
    http.del(`${baseUrl}/api/v1/cart`, null, {
      headers: { 'Authorization': `Bearer ${user.token}`, 'X-Bypass-Rate-Limit': 'true' }
    });
  }
}
