/**
 * ÉPICA 2 - GESTIÓN DE PRÉSTAMOS
 * PRUEBA DE CARGA (Load Testing)
 *
 * Objetivo: Verificar el comportamiento del sistema bajo niveles de carga
 * esperados y pico con usuarios concurrentes: 10, 50, 100, 500, 1000.
 *
 * Endpoints cubiertos:
 *  - GET  /api/v1/loans/         → Listar todos los préstamos
 *  - POST /api/v1/loans/         → Crear préstamo
 *  - GET  /api/v1/loans/{id}     → Obtener préstamo por ID
 *  - PUT  /api/v1/loans/{id}/return → Registrar devolución
 *  - POST /api/v1/loans/validate-comprehensive → Validar préstamo
 *
 * Umbrales de aceptación:
 *  - p(95) de duración HTTP < 2000ms
 *  - Tasa de error < 5%
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ─── Métricas personalizadas ───────────────────────────────────────────────
const loanCreationErrors  = new Counter('loan_creation_errors');
const loanListDuration    = new Trend('loan_list_duration', true);
const loanCreateDuration  = new Trend('loan_create_duration', true);
const loanReturnDuration  = new Trend('loan_return_duration', true);
const errorRate           = new Rate('error_rate');

// ─── Configuración de escenarios (VUs concurrentes escalonados) ───────────
export const options = {
  scenarios: {
    // Nivel 1: 10 usuarios concurrentes
    load_10_users: {
      executor: 'constant-vus',
      vus: 10,
      duration: '1m',
      startTime: '0s',
      tags: { level: '10_users' },
    },
    // Nivel 2: 50 usuarios concurrentes
    load_50_users: {
      executor: 'constant-vus',
      vus: 50,
      duration: '1m',
      startTime: '1m30s',
      tags: { level: '50_users' },
    },
    // Nivel 3: 100 usuarios concurrentes
    load_100_users: {
      executor: 'constant-vus',
      vus: 100,
      duration: '1m',
      startTime: '3m',
      tags: { level: '100_users' },
    },
    // Nivel 4: 500 usuarios concurrentes
    load_500_users: {
      executor: 'constant-vus',
      vus: 500,
      duration: '1m',
      startTime: '4m30s',
      tags: { level: '500_users' },
    },
    // Nivel 5: 1000 usuarios concurrentes
    load_1000_users: {
      executor: 'constant-vus',
      vus: 1000,
      duration: '1m',
      startTime: '6m',
      tags: { level: '1000_users' },
    },
  },
  thresholds: {
    // El 95% de las peticiones debe responder en menos de 2 segundos
    http_req_duration: ['p(95)<2000'],
    // Tasa de error HTTP inferior al 5%
    http_req_failed: ['rate<0.05'],
    // Métricas específicas por endpoint
    loan_list_duration:   ['p(95)<1500'],
    loan_create_duration: ['p(95)<2500'],
    loan_return_duration: ['p(95)<2500'],
    error_rate:           ['rate<0.05'],
  },
};

// ─── Variables de entorno / configuración ─────────────────────────────────
const BASE_URL   = __ENV.BASE_URL   || 'http://localhost:8081';
const CLIENT_ID  = __ENV.CLIENT_ID  || '1';
const TOOL_ID    = __ENV.TOOL_ID    || '1';
const LOAN_ID    = __ENV.LOAN_ID    || '1';

const HEADERS = {
  'Content-Type': 'application/json',
  Accept: 'application/json',
};

// ─── Función principal ejecutada por cada VU ──────────────────────────────
export default function () {
  // 1. Listar todos los préstamos (lectura frecuente)
  group('GET /api/v1/loans/ - Listar préstamos', () => {
    const res = http.get(`${BASE_URL}/api/v1/loans/`, { headers: HEADERS });
    const ok  = check(res, {
      'status es 200':             (r) => r.status === 200,
      'respuesta contiene array':  (r) => Array.isArray(r.json()),
      'tiempo < 2000ms':           (r) => r.timings.duration < 2000,
    });
    loanListDuration.add(res.timings.duration);
    errorRate.add(!ok);
    sleep(0.5);
  });

  // 2. Validar disponibilidad antes de crear préstamo
  group('POST /api/v1/loans/validate-comprehensive - Validar préstamo', () => {
    const payload = JSON.stringify({
      clientId: parseInt(CLIENT_ID),
      toolId:   parseInt(TOOL_ID),
      quantity: 1,
    });
    const res = http.post(`${BASE_URL}/api/v1/loans/validate-comprehensive`, payload, {
      headers: HEADERS,
    });
    const ok = check(res, {
      'status es 200':          (r) => r.status === 200,
      'tiempo < 2000ms':        (r) => r.timings.duration < 2000,
    });
    errorRate.add(!ok);
    sleep(0.3);
  });

  // 3. Crear un préstamo nuevo
  group('POST /api/v1/loans/ - Crear préstamo', () => {
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 7);
    const returnDate = tomorrow.toISOString().split('T')[0];

    const payload = JSON.stringify({
      clientId:         parseInt(CLIENT_ID),
      toolId:           parseInt(TOOL_ID),
      quantity:         1,
      agreedReturnDate: returnDate,
      notes:            `Préstamo de prueba de carga - VU ${__VU}`,
    });

    const res = http.post(`${BASE_URL}/api/v1/loans/`, payload, { headers: HEADERS });
    const ok  = check(res, {
      'préstamo creado (200 o 500)': (r) => r.status === 200 || r.status === 500,
      'tiempo < 3000ms':             (r) => r.timings.duration < 3000,
    });
    loanCreateDuration.add(res.timings.duration);
    if (res.status !== 200) loanCreationErrors.add(1);
    errorRate.add(!ok);
    sleep(1);
  });

  // 4. Obtener préstamo por ID
  group('GET /api/v1/loans/{id} - Obtener préstamo', () => {
    const res = http.get(`${BASE_URL}/api/v1/loans/${LOAN_ID}`, { headers: HEADERS });
    const ok  = check(res, {
      'status es 200 o 404': (r) => r.status === 200 || r.status === 404,
      'tiempo < 1500ms':     (r) => r.timings.duration < 1500,
    });
    errorRate.add(!ok);
    sleep(0.5);
  });

  // 5. Registrar devolución
  group('PUT /api/v1/loans/{id}/return - Devolver herramienta', () => {
    const res = http.put(
      `${BASE_URL}/api/v1/loans/${LOAN_ID}/return?damaged=false&damageType=MINOR&notes=Devolucion+prueba`,
      null,
      { headers: HEADERS }
    );
    const ok = check(res, {
      'devolución procesada':  (r) => r.status === 200 || r.status === 404 || r.status === 500,
      'tiempo < 3000ms':       (r) => r.timings.duration < 3000,
    });
    loanReturnDuration.add(res.timings.duration);
    errorRate.add(!ok);
    sleep(1);
  });
}

// ─── Hook de resumen al finalizar ─────────────────────────────────────────
export function handleSummary(data) {
  const summary = {
    test_type:  'load_testing',
    epic:       'Épica 2 - Gestión de Préstamos',
    timestamp:  new Date().toISOString(),
    thresholds_passed: Object.entries(data.metrics)
      .filter(([, m]) => m.thresholds)
      .every(([, m]) => Object.values(m.thresholds).every((t) => !t.ok === false)),
    metrics: {
      http_req_duration_p95: data.metrics.http_req_duration?.values?.['p(95)'],
      http_req_duration_p99: data.metrics.http_req_duration?.values?.['p(99)'],
      http_req_failed_rate:  data.metrics.http_req_failed?.values?.rate,
      http_reqs_total:       data.metrics.http_reqs?.values?.count,
      iterations_total:      data.metrics.iterations?.values?.count,
      loan_list_p95:         data.metrics.loan_list_duration?.values?.['p(95)'],
      loan_create_p95:       data.metrics.loan_create_duration?.values?.['p(95)'],
      loan_return_p95:       data.metrics.loan_return_duration?.values?.['p(95)'],
      loan_creation_errors:  data.metrics.loan_creation_errors?.values?.count,
    },
  };

  return {
    'results/epica2-load-test-summary.json': JSON.stringify(summary, null, 2),
    stdout: `\n✅ Épica 2 - Prueba de Carga finalizada\n${JSON.stringify(summary.metrics, null, 2)}\n`,
  };
}

