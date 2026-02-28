/**
 * ÉPICA 6 - KARDEX DE MOVIMIENTOS
 * PRUEBA DE ESTRÉS (Stress Testing)
 *
 * Objetivo: Identificar el punto de quiebre del módulo de Kardex bajo carga
 * extrema. El Kardex es especialmente vulnerable porque:
 *  - Cada préstamo/devolución genera escrituras simultáneas en kardex + stock
 *  - Las consultas de rango de fechas son costosas con muchos registros
 *  - La consistencia transaccional puede generar bloqueos en la BD
 *
 * Punto de quiebre esperado: cuando las escrituras concurrentes generan
 * deadlocks o timeouts de transacción en PostgreSQL.
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ─── Métricas personalizadas ───────────────────────────────────────────────
const concurrentWriteErrors = new Counter('concurrent_write_errors');
const deadlockErrors        = new Counter('deadlock_errors');
const timeoutErrors         = new Counter('timeout_errors');
const successRate           = new Rate('success_rate');
const writeLatency          = new Trend('write_latency', true);
const readLatency           = new Trend('read_latency', true);

// ─── Configuración de estrés ──────────────────────────────────────────────
export const options = {
  stages: [
    // Calentamiento
    { duration: '1m',   target: 30   },
    { duration: '30s',  target: 30   },
    // Estrés progresivo
    { duration: '1m',   target: 100  },
    { duration: '30s',  target: 100  },
    { duration: '1m',   target: 250  },
    { duration: '30s',  target: 250  },
    { duration: '1m',   target: 500  },
    { duration: '30s',  target: 500  },  // Zona de posibles deadlocks
    { duration: '1m',   target: 800  },
    { duration: '30s',  target: 800  },
    { duration: '1m',   target: 1200 },
    { duration: '2m',   target: 1200 },  // Punto de quiebre esperado
    // Recuperación
    { duration: '1m',   target: 400  },
    { duration: '1m',   target: 100  },
    { duration: '1m',   target: 0    },
  ],
  thresholds: {
    http_req_duration:         ['p(99)<15000'],
    http_req_failed:           ['rate<0.60'],   // Permisivo — objetivo es el punto de quiebre
    success_rate:              ['rate>0.30'],
    concurrent_write_errors:   ['count<999999'],
    deadlock_errors:           ['count<999999'],
  },
};

// ─── Configuración ─────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const TOOL_ID  = __ENV.TOOL_ID  || '1';
const LOAN_ID  = __ENV.LOAN_ID  || '1';

const HEADERS = { 'Content-Type': 'application/json', Accept: 'application/json' };

// ─── Función principal ────────────────────────────────────────────────────
export default function () {
  // Mezcla agresiva de lecturas y escrituras para forzar contención en la BD
  group('Kardex - Estrés de escrituras concurrentes', () => {

    // Escritura 1: Movimiento de préstamo (actualiza kardex + stock)
    const writeLoan = http.post(
      `${BASE_URL}/api/kardex-movements/loan`,
      JSON.stringify({
        toolId:      parseInt(TOOL_ID),
        quantity:    1,
        description: `Estrés préstamo VU=${__VU} iter=${__ITER}`,
        loanId:      parseInt(LOAN_ID),
      }),
      { headers: HEADERS }
    );
    writeLatency.add(writeLoan.timings.duration);

    const writeOk = check(writeLoan, {
      'escritura sin error fatal': (r) => r.status !== 503 && r.status !== 0,
    });
    successRate.add(writeOk);
    if (!writeOk) concurrentWriteErrors.add(1);
    // Detectar deadlocks (PostgreSQL: error code 40P01)
    if (writeLoan.body && writeLoan.body.includes('deadlock')) deadlockErrors.add(1);
    if (writeLoan.body && writeLoan.body.includes('timeout'))  timeoutErrors.add(1);

    // Lectura concurrente mientras se escribe (simula operador consultando)
    const readRes = http.get(
      `${BASE_URL}/api/kardex-movements/tool/${TOOL_ID}`,
      { headers: HEADERS }
    );
    readLatency.add(readRes.timings.duration);
    const readOk = check(readRes, {
      'lectura bajo estrés responde': (r) => r.status < 500,
    });
    successRate.add(readOk);

    // Escritura 2: Movimiento de devolución (misma herramienta — contención máxima)
    const writeReturn = http.post(
      `${BASE_URL}/api/kardex-movements/return`,
      JSON.stringify({
        toolId:      parseInt(TOOL_ID),
        quantity:    1,
        description: `Estrés devolución VU=${__VU}`,
        userId:      1,
        loanId:      parseInt(LOAN_ID),
        instanceIds: [],
        isDamaged:   false,
      }),
      { headers: HEADERS }
    );
    writeLatency.add(writeReturn.timings.duration);

    if (!writeReturn.body?.includes('deadlock') === false) deadlockErrors.add(1);
    if (writeReturn.status >= 500) concurrentWriteErrors.add(1);

    // Escritura 3: Reabastecimiento (actualiza stock — conflicto con préstamo)
    const writeRestock = http.post(
      `${BASE_URL}/api/kardex-movements/restock`,
      JSON.stringify({
        toolId:      parseInt(TOOL_ID),
        quantity:    2,
        description: `Estrés reabastecimiento VU=${__VU}`,
        userId:      1,
      }),
      { headers: HEADERS }
    );
    writeLatency.add(writeRestock.timings.duration);
    if (writeRestock.status >= 500) concurrentWriteErrors.add(1);
  });

  // Sin sleep — máxima presión
  sleep(0.05);
}

// ─── Resumen ──────────────────────────────────────────────────────────────
export function handleSummary(data) {
  const p95  = data.metrics.http_req_duration?.values?.['p(95)'] ?? 'N/A';
  const p99  = data.metrics.http_req_duration?.values?.['p(99)'] ?? 'N/A';
  const errR = data.metrics.http_req_failed?.values?.rate ?? 'N/A';
  const rps  = data.metrics.http_reqs?.values?.rate ?? 'N/A';
  const dlE  = data.metrics.deadlock_errors?.values?.count ?? 0;
  const cwE  = data.metrics.concurrent_write_errors?.values?.count ?? 0;
  const toE  = data.metrics.timeout_errors?.values?.count ?? 0;

  const summary = {
    test_type: 'stress_testing',
    epic:      'Épica 6 - Kardex de Movimientos',
    timestamp: new Date().toISOString(),
    breaking_point_analysis: {
      p95_ms:                 p95,
      p99_ms:                 p99,
      error_rate:             errR,
      requests_per_second:    rps,
      deadlock_errors:        dlE,
      concurrent_write_errors: cwE,
      timeout_errors:         toE,
      interpretation:
        dlE > 0
          ? `⚠️  DEADLOCKS DETECTADOS (${dlE}) — La BD sufre contención por escrituras concurrentes sobre la misma herramienta`
          : errR > 0.3
          ? `⚠️  PUNTO DE QUIEBRE — Tasa de errores ${(errR * 100).toFixed(1)}% supera umbral de 30%`
          : p99 > 8000
          ? `⚠️  LATENCIA CRÍTICA — p(99) = ${p99}ms indica saturación del servidor`
          : '✅ Sistema resistió la carga de estrés en el módulo Kardex',
    },
    max_vus_tested: 1200,
    recomendaciones:
      dlE > 0
        ? 'Revisar transacciones en KardexMovementService — considerar retry con backoff exponencial'
        : 'Sistema opera dentro de los parámetros esperados bajo estrés',
  };

  return {
    'results/epica6-stress-test-summary.json': JSON.stringify(summary, null, 2),
    stdout: `\n⚡ Épica 6 - Prueba de Estrés finalizada\n${JSON.stringify(summary.breaking_point_analysis, null, 2)}\n`,
  };
}

