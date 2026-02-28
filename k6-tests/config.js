/**
 * CONFIGURACIÓN COMPARTIDA PARA TODOS LOS TESTS K6
 * toolrent - Pruebas de Rendimiento
 *
 * Este archivo centraliza:
 *  - URLs base del sistema
 *  - Headers comunes
 *  - Datos de prueba reutilizables
 *  - Funciones utilitarias
 */

// ─── URLs base ─────────────────────────────────────────────────────────────
export const BASE_URL    = __ENV.BASE_URL    || 'http://localhost:8081';
export const FRONTEND_URL= __ENV.FRONTEND_URL|| 'http://localhost:8070';

// ─── IDs de entidades de prueba (deben existir en la BD) ─────────────────
// IMPORTANTE: Ejecutar primero el seed de datos antes de las pruebas
export const TEST_CLIENT_ID = parseInt(__ENV.CLIENT_ID  || '1');
export const TEST_TOOL_ID   = parseInt(__ENV.TOOL_ID    || '1');
export const TEST_LOAN_ID   = parseInt(__ENV.LOAN_ID    || '1');

// ─── Headers HTTP comunes ──────────────────────────────────────────────────
export const HEADERS = {
  'Content-Type': 'application/json',
  Accept:         'application/json',
};

// ─── Umbrales de rendimiento estándar ─────────────────────────────────────
export const THRESHOLDS = {
  standard: {
    http_req_duration: ['p(95)<2000'],
    http_req_failed:   ['rate<0.05'],
  },
  strict: {
    http_req_duration: ['p(95)<500'],
    http_req_failed:   ['rate<0.01'],
  },
  permissive: {
    http_req_duration: ['p(95)<5000'],
    http_req_failed:   ['rate<0.20'],
  },
};

// ─── Utilidades de fecha ──────────────────────────────────────────────────
/**
 * Retorna fecha futura en formato YYYY-MM-DD
 * @param {number} daysFromNow - días desde hoy
 */
export function futureDate(daysFromNow = 7) {
  const d = new Date();
  d.setDate(d.getDate() + daysFromNow);
  return d.toISOString().split('T')[0];
}

/**
 * Retorna rango de fechas para consultas de kardex
 * @param {number} daysBack - días hacia atrás
 */
export function dateRange(daysBack = 30) {
  const end   = new Date();
  const start = new Date();
  start.setDate(start.getDate() - daysBack);
  return {
    startDate: start.toISOString().split('T')[0],
    endDate:   end.toISOString().split('T')[0],
  };
}

/**
 * Retorna un número entero aleatorio entre min y max (inclusivo)
 */
export function randomInt(min, max) {
  return Math.floor(Math.random() * (max - min + 1)) + min;
}

// ─── Payloads de prueba reutilizables ────────────────────────────────────

/** Payload para crear un préstamo */
export function loanPayload(clientId = TEST_CLIENT_ID, toolId = TEST_TOOL_ID, notes = '') {
  return JSON.stringify({
    clientId,
    toolId,
    quantity:         1,
    agreedReturnDate: futureDate(7),
    notes:            notes || `Test k6 VU=${__VU}`,
  });
}

/** Payload para movimiento de préstamo en kardex */
export function kardexLoanPayload(toolId = TEST_TOOL_ID, loanId = TEST_LOAN_ID) {
  return JSON.stringify({
    toolId,
    quantity:    1,
    description: `Mov préstamo k6 VU=${__VU}`,
    loanId,
  });
}

/** Payload para movimiento de devolución en kardex */
export function kardexReturnPayload(toolId = TEST_TOOL_ID, loanId = TEST_LOAN_ID) {
  return JSON.stringify({
    toolId,
    quantity:    1,
    description: `Mov devolución k6 VU=${__VU}`,
    userId:      1,
    loanId,
    instanceIds: [],
    isDamaged:   false,
  });
}

/** Payload para reabastecimiento en kardex */
export function kardexRestockPayload(toolId = TEST_TOOL_ID, quantity = 5) {
  return JSON.stringify({
    toolId,
    quantity,
    description: `Reabastecimiento k6 VU=${__VU}`,
    userId:      1,
  });
}

// ─── Formateo de resúmenes ────────────────────────────────────────────────
/**
 * Genera un objeto de resumen estándar para handleSummary
 */
export function buildSummary(testType, epic, data, extraFields = {}) {
  return {
    test_type:  testType,
    epic,
    timestamp:  new Date().toISOString(),
    metrics: {
      http_req_duration_p95: data.metrics.http_req_duration?.values?.['p(95)'],
      http_req_duration_p99: data.metrics.http_req_duration?.values?.['p(99)'],
      http_req_failed_rate:  data.metrics.http_req_failed?.values?.rate,
      http_reqs_total:       data.metrics.http_reqs?.values?.count,
      iterations_total:      data.metrics.iterations?.values?.count,
      vus_max:               data.metrics.vus_max?.values?.max,
    },
    ...extraFields,
  };
}

