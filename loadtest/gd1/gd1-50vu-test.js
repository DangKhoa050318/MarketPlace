import http from 'k6/http';
import { check, sleep } from 'k6';

const rampStage50 = [
  { duration: '20s', target: 50 },
  { duration: '40s', target: 50 },
  { duration: '20s', target: 0 },
];

export const options = {
  scenarios: {
    products_50vu: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: rampStage50,
      exec: 'testProducts',
    },
    catalog_50vu: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: rampStage50,
      exec: 'testCatalog',
    },
    recommendations_50vu: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: rampStage50,
      exec: 'testRecommendations',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.05'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const headers = { 'X-Bypass-Rate-Limit': 'true' };

export function testProducts() {
  const randomPage = Math.floor(Math.random() * 6);
  const res = http.get(`${baseUrl}/api/v1/products?page=${randomPage}&size=10&sortBy=createdAt&sortDir=DESC`, { headers });
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(0.5);
}

export function testCatalog() {
  const minPrice = Math.floor(Math.random() * 50) * 10000;
  const maxPrice = minPrice + 500000;
  const res = http.get(`${baseUrl}/api/v1/products/catalog?page=0&size=10&minPrice=${minPrice}&maxPrice=${maxPrice}`, { headers });
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(0.5);
}

export function testRecommendations() {
  const res = http.get(`${baseUrl}/api/v1/recommendations?placement=HOME_BEST_SELLERS&limit=12`, { headers });
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(0.5);
}
