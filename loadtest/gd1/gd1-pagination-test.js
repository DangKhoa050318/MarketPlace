import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  scenarios: {
    size_10_scenario: {
      executor: 'constant-vus',
      vus: 20,
      duration: '30s',
      exec: 'testSize10',
    },
    size_100_scenario: {
      executor: 'constant-vus',
      vus: 20,
      duration: '30s',
      startTime: '35s',
      exec: 'testSize100',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{scenario:size_10_scenario}': ['p(95)<500'],
    'http_req_duration{scenario:size_100_scenario}': ['p(95)<500'],
  },
};

const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
const headers = { 'X-Bypass-Rate-Limit': 'true' };

export function testSize10() {
  const randomPage = Math.floor(Math.random() * 5);
  const res = http.get(`${baseUrl}/api/v1/products?page=${randomPage}&size=10&sortBy=createdAt&sortDir=DESC`, { headers });
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(0.5);
}

export function testSize100() {
  const res = http.get(`${baseUrl}/api/v1/products?page=0&size=100&sortBy=createdAt&sortDir=DESC`, { headers });
  check(res, { 'status is 200': (r) => r.status === 200 });
  sleep(0.5);
}
