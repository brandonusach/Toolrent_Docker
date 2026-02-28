/**
 * ÉPICA 2 - GESTIÓN DE PRÉSTAMOS
 * PRUEBA DE ESTRÉS (Stress Testing)
 *
 * Objetivo: Llevar el sistema más allá de su capacidad normal para identificar
 * el punto de quiebre (breaking point). Se incrementa gradualmente la carga
 * hasta que el sistema falle o se degrade significativamente.
 *
 * Etapas:
 *  1. Rampa de subida agresiva hasta 1500 VUs
 *  2. Mantener carga máxima durante 2 minutos
 *  3. Descenso gradual (cooldown)
 *
 * Métricas clave para identificar punto de quiebre:
 *  - Incremento brusco en p(99) de latencia
 *  - Tasa de errores > 10%
 *  - Throughput (req/s) que deja de crecer o decrece
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend, Gauge } from 'k6/metrics';

// ─── Métricas personalizadas ───────────────────────────────────────────────
const breakingPointErrors = new Counter('breaking_point_errors');
const serverErrors        = new Counter('server_errors_5xx');
const successRate         = new Rate('success_rate');
const latencyTrend        = new Trend('request_latency', true);

// ─── Configuración de escenarios de estrés ────────────────────────────────
export const options = {
  stages: [
    // Calentamiento normal
    { duration: '1m',   target: 50   },  // Subida a carga normal
    { duration: '30s',  target: 50   },  // Carga estable
    // Inicio de estrés - incremento agresivo
    { duration: '1m',   target: 200  },  // Estrés moderado
    { duration: '30s',  target: 200  },  // Sostener
    { duration: '1m',   target: 400  },  // Estrés alto
    { duration: '30s',  target: 400  },  // Sostener
    { duration: '1m',   target: 700  },  // Estrés severo
    { duration: '30s',  target: 700  },  // Sostener — zona de quiebre esperada
    { duration: '1m',   target: 1000 },  // Estrés extremo
    { duration: '30s',  target: 1000 },  // Sostener al máximo
    { duration: '1m',   target: 1500 },  // Más allá del límite
    { duration: '2m',   target: 1500 },  // Mantener en el punto de quiebre
    // Recuperación
    { duration: '1m',   target: 500  },  // Descenso parcial
    { duration: '1m',   target: 100  },  // Descenso mayor
    { duration: '1m',   target: 0    },  // Apagado gradual
  ],
  thresholds: {
    // Umbrales más permisivos — el objetivo es ENCONTRAR el punto de quiebre
    http_req_duration:  ['p(99)<10000'],   // Solo falla si supera 10s (p99)
    http_req_failed:    ['rate<0.50'],     // Acepta hasta 50% de errores en estrés
    success_rate:       ['rate>0.40'],     // Al menos 40% de éxitos esperado
    // Umbrales informativos
    breaking_point_errors: ['count<999999'],
  },
};

// ─── Configuración ─────────────────────────────────────────────────────────
const BASE_URL  = __ENV.BASE_URL  || 'http://localhost:8081';
const CLIENT_ID = __ENV.CLIENT_ID || '1';
const TOOL_ID   = __ENV.TOOL_ID   || '1';
const LOAN_ID   = __ENV.LOAN_ID   || '1';

const HEADERS = {
  'Content-Type': 'application/json',
  Accept: 'application/json',
};

// ─── Función principal ────────────────────────────────────────────────────
export default function () {
  // Flujo crítico: el más costoso en la épica de préstamos
  group('Flujo completo de préstamo (Estrés)', () => {

    // 1. Validación previa (consulta BD)
    const validateRes = http.post(
      `${BASE_URL}/api/v1/loans/validate-comprehensive`,
      JSON.stringify({
        clientId: parseInt(CLIENT_ID),
        toolId:   parseInt(TOOL_ID),
        quantity: 1,
      }),
      { headers: HEADERS }
    );
    latencyTrend.add(validateRes.timings.duration);
    const validateOk = check(validateRes, {
      'validación responde': (r) => r.status < 500,
    });
    if (!validateOk) {
      serverErrors.add(1);
      breakingPointErrors.add(1);
    }
    successRate.add(validateOk);

    // 2. Creación de préstamo (escritura en BD)
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 7);
    const returnDate = tomorrow.toISOString().split('T')[0];

    const createRes = http.post(
      `${BASE_URL}/api/v1/loans/`,
      JSON.stringify({
        clientId:         parseInt(CLIENT_ID),
        toolId:           parseInt(TOOL_ID),
        quantity:         1,
        agreedReturnDate: returnDate,
        notes:            `Stress test VU-${__VU} iter-${__ITER}`,
      }),
      { headers: HEADERS }
    );
    latencyTrend.add(createRes.timings.duration);
    const createOk = check(createRes, {
      'préstamo creado sin error 5xx': (r) => r.status !== 500 && r.status !== 503,
    });
    if (!createOk) {
      serverErrors.add(1);
      breakingPointErrors.add(1);
    }
    successRate.add(createOk);

    // 3. Consulta de listado (lecturas concurrentes)
    const listRes = http.get(`${BASE_URL}/api/v1/loans/`, { headers: HEADERS });
    latencyTrend.add(listRes.timings.duration);
    const listOk = check(listRes, {
      'listado responde': (r) => r.status < 500,
    });
    if (!listOk) {
      serverErrors.add(1);
      breakingPointErrors.add(1);
    }
    successRate.add(listOk);

    // 4. Devolución (escritura concurrente + actualización de stock)
    const returnRes = http.put(
      `${BASE_URL}/api/v1/loans/${LOAN_ID}/return?damaged=false&damageType=MINOR`,
      null,
      { headers: HEADERS }
    );
    latencyTrend.add(returnRes.timings.duration);
    const returnOk = check(returnRes, {
      'devolución responde': (r) => r.status !== 503,
    });
    if (!returnOk) {
      serverErrors.add(1);
      breakingPointErrors.add(1);
    }
    successRate.add(returnOk);
  });

  // Pausa mínima — simular usuarios bajo presión real
  sleep(0.1);
}

// ─── Resumen ──────────────────────────────────────────────────────────────
export function handleSummary(data) {
  const p95  = data.metrics.http_req_duration?.values?.['p(95)'] ?? 'N/A';
  const p99  = data.metrics.http_req_duration?.values?.['p(99)'] ?? 'N/A';
  const errR = data.metrics.http_req_failed?.values?.rate ?? 'N/A';
  const rps  = data.metrics.http_reqs?.values?.rate ?? 'N/A';
  const bpE  = data.metrics.breaking_point_errors?.values?.count ?? 0;

  const summary = {
    test_type:  'stress_testing',
    epic:       'Épica 2 - Gestión de Préstamos',
    timestamp:  new Date().toISOString(),
    breaking_point_analysis: {
      description: 'El punto de quiebre se identifica cuando p(99) supera 5000ms O error_rate supera 20%',
      p95_ms:               p95,
      p99_ms:               p99,
      error_rate:           errR,
      requests_per_second:  rps,
      breaking_point_errors: bpE,
      interpretation:
        errR > 0.2
          ? '⚠️  PUNTO DE QUIEBRE ALCANZADO — tasa de errores supera 20%'
          : p99 > 5000
          ? '⚠️  DEGRADACIÓN SEVERA — latencia p(99) supera 5000ms'
          : '✅ Sistema resistió la carga de estrés sin punto de quiebre claro',
    },
    max_vus_tested: 1500,
    metrics: {
      http_req_duration_p95: p95,
      http_req_duration_p99: p99,
      http_req_failed_rate:  errR,
      http_reqs_total:       data.metrics.http_reqs?.values?.count,
      breaking_point_errors: bpE,
      server_errors_5xx:     data.metrics.server_errors_5xx?.values?.count,
    },
  };

  return {
    'results/epica2-stress-test-summary.json': JSON.stringify(summary, null, 2),
    stdout: `\n⚡ Épica 2 - Prueba de Estrés finalizada\n${JSON.stringify(summary.breaking_point_analysis, null, 2)}\n`,
  };
}

