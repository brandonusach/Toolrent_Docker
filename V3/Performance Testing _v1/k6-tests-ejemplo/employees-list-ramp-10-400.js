import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 10 },
    { duration: '30s', target: 50 },
    { duration: '30s', target: 100 },
    { duration: '30s', target: 200 },
    { duration: '30s', target: 400 },
    { duration: '30s', target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],      
    http_req_duration: ["p(95)<500"],  
  },
};

const BASE_URL = "http://localhost:8090";

export default function () {
  const res = http.get(`${BASE_URL}/api/v1/employees/`);

  check(res, {
    "status is 200": (r) => r.status === 200,
    "response is array": (r) => Array.isArray(r.json()),
  });

  sleep(1);
}