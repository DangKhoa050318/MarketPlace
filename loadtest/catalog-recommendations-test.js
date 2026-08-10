import http from 'k6/http';
import { check, sleep } from 'k6';

// 1. Cấu hình các kịch bản tải (Load Profiles) khác nhau
const profiles = {
  // Smoke: Test nhanh xem API hoạt động đúng không với 1 Virtual User (VU)
  smoke: {
    vus: 1,
    duration: '10s',
  },
  // Load: Test tải bình thường (tăng dần lên 10 VUs, duy trì, rồi giảm dần)
  load: {
    stages: [
      { duration: '10s', target: 5 },  // ramp-up lên 5 users
      { duration: '20s', target: 10 }, // duy trì ở 10 users
      { duration: '10s', target: 0 },  // ramp-down về 0
    ],
  },
  // Stress: Test xem giới hạn tải tối đa của API là bao nhiêu
  stress: {
    stages: [
      { duration: '10s', target: 10 },
      { duration: '20s', target: 30 },
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

// Chọn profile chạy dựa trên biến môi trường PROFILE (mặc định chạy smoke)
const profileName = __ENV.PROFILE || 'smoke';
const selectedProfile = profiles[profileName] || profiles.smoke;

export const options = Object.assign({
  thresholds: {
    http_req_failed: ['rate<0.01'],      // Tỷ lệ request lỗi < 1%
    http_req_duration: ['p(95)<1500'],   // 95% request phải hoàn thành dưới 1.5 giây
  }
}, selectedProfile);

export default function () {
  const baseUrl = __ENV.BASE_URL || 'http://localhost:8080';
  
  // GĐ1: Lấy danh sách sản phẩm (Catalog)
  const catalogRes = http.get(`${baseUrl}/api/v1/products/catalog?page=0&size=20`);
  check(catalogRes, {
    'GET /catalog status is 200': (r) => r.status === 200,
    'GET /catalog has content': (r) => {
      try {
        const body = JSON.parse(r.body);
        return body && body.data !== undefined;
      } catch (e) {
        return false;
      }
    }
  });

  sleep(1);

  // GĐ1: Lấy danh sách sản phẩm gợi ý (Recommendations)
  const recRes = http.get(`${baseUrl}/api/v1/recommendations?limit=10`);
  check(recRes, {
    'GET /recommendations status is 200': (r) => r.status === 200,
  });

  sleep(1);
}
