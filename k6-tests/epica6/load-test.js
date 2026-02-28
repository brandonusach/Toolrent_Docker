/**
 * ÉPICA 6 - KARDEX DE MOVIMIENTOS
 * PRUEBA DE CARGA (Load Testing)
 *
 * Objetivo: Verificar el comportamiento del módulo de Kardex bajo niveles
 * de usuarios concurrentes: 10, 50, 100, 500, 1000.
 *
 * El Kardex es un módulo de lectura intensiva — los operadores consultan
 * historial de movimientos con filtros frecuentemente.
 *
 * Endpoints cubiertos:
 *  - GET  /api/kardex-movements/tool/{toolId}         → Movimientos por herramienta
 *  - GET  /api/kardex-movements/date-range            → Reporte por rango de fechas
 *  - GET  /api/kardex-movements/                      → Todos los movimientos
 *  - POST /api/kardex-movements/loan                  → Registrar movimiento préstamo
 *  - POST /api/kardex-movements/return                → Registrar movimiento devolución
 *  - POST /api/kardex-movements/restock               → Registrar reabastecimiento
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ─── Métricas personalizadas ───────────────────────────────────────────────
const kardexReadDuration   = new Trend('kardex_read_duration', true);
const kardexWriteDuration  = new Trend('kardex_write_duration', true);
const kardexFilterDuration = new Trend('kardex_filter_duration', true);
const kardexErrors         = new Counter('kardex_errors');
const errorRate            = new Rate('error_rate');

// ─── Configuración de escenarios ──────────────────────────────────────────
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
    http_req_duration:      ['p(95)<2000'],
    http_req_failed:        ['rate<0.05'],
    kardex_read_duration:   ['p(95)<1000'],   // Lecturas deben ser rápidas
    kardex_write_duration:  ['p(95)<2500'],   // Escrituras pueden ser más lentas
    kardex_filter_duration: ['p(95)<2000'],   // Filtros con índices
    error_rate:             ['rate<0.05'],
  },
};

// ─── Configuración ─────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const TOOL_ID  = __ENV.TOOL_ID  || '1';
const LOAN_ID  = __ENV.LOAN_ID  || '1';

const HEADERS = { 'Content-Type': 'application/json', Accept: 'application/json' };

// Fechas para filtros de rango
function getDateRange(daysBack = 30) {
  const end   = new Date();
  const start = new Date();
  start.setDate(start.getDate() - daysBack);
  return {
    startDate: start.toISOString().split('T')[0],
    endDate:   end.toISOString().split('T')[0],
  };
}

// ─── Función principal ────────────────────────────────────────────────────
export default function () {
  // Distribución realista: 70% lecturas, 30% escrituras
  const rand = Math.random();

  if (rand < 0.35) {
    // 35%: Consulta de movimientos por herramienta
    group('GET /api/kardex-movements/tool/{id} - Por herramienta', () => {
      const res = http.get(
        `${BASE_URL}/api/kardex-movements/tool/${TOOL_ID}`,
        { headers: HEADERS }
      );
      const ok = check(res, {
        'status 200 o 404':    (r) => r.status === 200 || r.status === 404,
        'tiempo < 1000ms':     (r) => r.timings.duration < 1000,
      });
      kardexReadDuration.add(res.timings.duration);
      if (res.status >= 500) kardexErrors.add(1);
      errorRate.add(!ok);
      sleep(0.5);
    });

  } else if (rand < 0.55) {
    // 20%: Reporte por rango de fechas (consulta con filtros — costosa)
    group('GET /api/kardex-movements/date-range - Reporte por fechas', () => {
      const { startDate, endDate } = getDateRange(30);
      const res = http.get(
        `${BASE_URL}/api/kardex-movements/date-range?startDate=${startDate}&endDate=${endDate}`,
        { headers: HEADERS }
      );
      const ok = check(res, {
        'status válido':       (r) => r.status === 200 || r.status === 400 || r.status === 404,
        'tiempo < 2000ms':     (r) => r.timings.duration < 2000,
      });
      kardexFilterDuration.add(res.timings.duration);
      if (res.status >= 500) kardexErrors.add(1);
      errorRate.add(!ok);
      sleep(0.5);
    });

  } else if (rand < 0.70) {
    // 15%: Listado completo de movimientos
    group('GET /api/kardex-movements/ - Listado completo', () => {
      const res = http.get(`${BASE_URL}/api/kardex-movements/`, { headers: HEADERS });
      const ok  = check(res, {
        'status 200 o 404':  (r) => r.status === 200 || r.status === 404,
        'tiempo < 2000ms':   (r) => r.timings.duration < 2000,
      });
      kardexReadDuration.add(res.timings.duration);
      if (res.status >= 500) kardexErrors.add(1);
      errorRate.add(!ok);
      sleep(0.5);
    });

  } else if (rand < 0.80) {
    // 10%: Registrar movimiento de préstamo (escritura)
    group('POST /api/kardex-movements/loan - Movimiento préstamo', () => {
      const res = http.post(
        `${BASE_URL}/api/kardex-movements/loan`,
        JSON.stringify({
          toolId:      parseInt(TOOL_ID),
          quantity:    1,
          description: `Préstamo carga VU=${__VU}`,
          loanId:      parseInt(LOAN_ID),
        }),
        { headers: HEADERS }
      );
      const ok = check(res, {
        'movimiento registrado': (r) => r.status === 201 || r.status === 400 || r.status === 200,
        'tiempo < 2500ms':       (r) => r.timings.duration < 2500,
      });
      kardexWriteDuration.add(res.timings.duration);
      if (res.status >= 500) kardexErrors.add(1);
      errorRate.add(!ok);
      sleep(1);
    });

  } else if (rand < 0.90) {
    // 10%: Registrar movimiento de devolución (escritura)
    group('POST /api/kardex-movements/return - Movimiento devolución', () => {
      const res = http.post(
        `${BASE_URL}/api/kardex-movements/return`,
        JSON.stringify({
          toolId:      parseInt(TOOL_ID),
          quantity:    1,
          description: `Devolución carga VU=${__VU}`,
          userId:      1,
          loanId:      parseInt(LOAN_ID),
          instanceIds: [],
          isDamaged:   false,
        }),
        { headers: HEADERS }
      );
      const ok = check(res, {
        'devolución registrada': (r) => r.status === 201 || r.status === 400 || r.status === 200,
        'tiempo < 2500ms':       (r) => r.timings.duration < 2500,
      });
      kardexWriteDuration.add(res.timings.duration);
      if (res.status >= 500) kardexErrors.add(1);
      errorRate.add(!ok);
      sleep(1);
    });

  } else {
    // 10%: Registrar reabastecimiento (escritura)
    group('POST /api/kardex-movements/restock - Reabastecimiento', () => {
      const res = http.post(
        `${BASE_URL}/api/kardex-movements/restock`,
        JSON.stringify({
          toolId:      parseInt(TOOL_ID),
          quantity:    5,
          description: `Reabastecimiento carga VU=${__VU}`,
          userId:      1,
        }),
        { headers: HEADERS }
      );
      const ok = check(res, {
        'reabastecimiento registrado': (r) => r.status === 201 || r.status === 400 || r.status === 200,
        'tiempo < 2500ms':             (r) => r.timings.duration < 2500,
      });
      kardexWriteDuration.add(res.timings.duration);
      if (res.status >= 500) kardexErrors.add(1);
      errorRate.add(!ok);
      sleep(1);
    });
  }
}

// ─── Resumen ──────────────────────────────────────────────────────────────
export function handleSummary(data) {
  const summary = {
    test_type: 'load_testing',
    epic:      'Épica 6 - Kardex de Movimientos',
    timestamp: new Date().toISOString(),
    metrics: {
      http_req_duration_p95:   data.metrics.http_req_duration?.values?.['p(95)'],
      http_req_duration_p99:   data.metrics.http_req_duration?.values?.['p(99)'],
      http_req_failed_rate:    data.metrics.http_req_failed?.values?.rate,
      http_reqs_total:         data.metrics.http_reqs?.values?.count,
      kardex_read_p95:         data.metrics.kardex_read_duration?.values?.['p(95)'],
      kardex_write_p95:        data.metrics.kardex_write_duration?.values?.['p(95)'],
      kardex_filter_p95:       data.metrics.kardex_filter_duration?.values?.['p(95)'],
      kardex_errors:           data.metrics.kardex_errors?.values?.count,
    },
  };

  return {
    'results/epica6-load-test-summary.json': JSON.stringify(summary, null, 2),
    stdout: `\n✅ Épica 6 - Prueba de Carga finalizada\n${JSON.stringify(summary.metrics, null, 2)}\n`,
  };
}

