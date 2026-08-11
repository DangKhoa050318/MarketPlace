import http from 'k6/http';
import { check, sleep } from 'k6';

const rampStage = [
  { duration: '30s', target: 20 },
  { duration: '1m', target: 20 },
  { duration: '30s', target: 0 },
];

export const options = {
  scenarios: {
    products_scenario: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: rampStage,
      exec: 'testProducts',
    },
    catalog_scenario: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: rampStage,
      exec: 'testCatalog',
    },
    recommendations_scenario: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: rampStage,
      exec: 'testRecommendations',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],              // Tỷ lệ lỗi < 1%
    http_req_duration: ['p(95)<500'],            // Response time p(95) < 500ms
    'http_req_duration{scenario:products_scenario}': ['p(95)<500'],
    'http_req_duration{scenario:catalog_scenario}': ['p(95)<500'],
    'http_req_duration{scenario:recommendations_scenario}': ['p(95)<500'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const headers = { 'X-Bypass-Rate-Limit': 'true' };

// Scenario 1: GET /api/v1/products với random page (0-5)
export function testProducts() {
  const pageSize = __ENV.PAGE_SIZE || 10;
  const randomPage = Math.floor(Math.random() * 6);
  const url = `${baseUrl}/api/v1/products?page=${randomPage}&size=${pageSize}&sortBy=createdAt&sortDir=DESC`;
  
  const res = http.get(url, { headers });
  
  check(res, {
    'GET /products status is 200': (r) => r.status === 200,
    'GET /products duration < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(0.5);
}

// Scenario 2: GET /api/v1/products/catalog với random query params
export function testCatalog() {
  const minPrice = Math.floor(Math.random() * 50) * 10000;
  const maxPrice = minPrice + 500000;
  const url = `${baseUrl}/api/v1/products/catalog?page=0&size=10&minPrice=${minPrice}&maxPrice=${maxPrice}`;
  
  const res = http.get(url, { headers });
  
  check(res, {
    'GET /catalog status is 200': (r) => r.status === 200,
    'GET /catalog duration < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(0.5);
}

// Scenario 3: GET /api/v1/recommendations
export function testRecommendations() {
  const url = `${baseUrl}/api/v1/recommendations?placement=HOME_BEST_SELLERS&limit=12`;
  
  const res = http.get(url, { headers });
  
  check(res, {
    'GET /recommendations status is 200': (r) => r.status === 200,
    'GET /recommendations duration < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(0.5);
}
