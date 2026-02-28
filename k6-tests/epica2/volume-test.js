/**
 * ÉPICA 2 - GESTIÓN DE PRÉSTAMOS
 * PRUEBA DE VOLUMEN (Volume Testing)
 *
 * Objetivo: Evaluar el comportamiento del sistema cuando la base de datos
 * contiene grandes volúmenes de datos (muchos préstamos registrados).
 * Se simulan escenarios con distintas densidades de datos:
 *   - Volumen bajo:    100 préstamos existentes
 *   - Volumen medio:   1.000 préstamos existentes
 *   - Volumen alto:    10.000 préstamos existentes
 *   - Volumen extremo: 100.000 préstamos existentes
 *
 * La prueba verifica:
 *  - Tiempo de respuesta en consultas con filtros/paginación vs sin ellos
 *  - Degradación de rendimiento conforme crece el volumen de datos
 *  - Comportamiento de las consultas de kardex acumuladas
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ─── Métricas personalizadas ───────────────────────────────────────────────
const volumeLowLatency    = new Trend('volume_low_latency', true);
const volumeMedLatency    = new Trend('volume_medium_latency', true);
const volumeHighLatency   = new Trend('volume_high_latency', true);
const volumeExtremeLatency= new Trend('volume_extreme_latency', true);
const dbErrors            = new Counter('db_errors');
const errorRate           = new Rate('error_rate');

// ─── Escenarios de volumen como etapas temporales ─────────────────────────
export const options = {
  scenarios: {
    // Fase 1: Volumen bajo — BD con ~100 registros (seed pequeño)
    volume_low: {
      executor: 'constant-vus',
      vus: 20,
      duration: '2m',
      startTime: '0s',
      env: { VOLUME_PHASE: 'low' },
      tags: { volume: 'low_100_records' },
    },
    // Fase 2: Volumen medio — BD con ~1.000 registros
    volume_medium: {
      executor: 'constant-vus',
      vus: 20,
      duration: '2m',
      startTime: '2m30s',
      env: { VOLUME_PHASE: 'medium' },
      tags: { volume: 'medium_1000_records' },
    },
    // Fase 3: Volumen alto — BD con ~10.000 registros
    volume_high: {
      executor: 'constant-vus',
      vus: 20,
      duration: '2m',
      startTime: '5m',
      env: { VOLUME_PHASE: 'high' },
      tags: { volume: 'high_10000_records' },
    },
    // Fase 4: Volumen extremo — BD con ~100.000 registros
    volume_extreme: {
      executor: 'constant-vus',
      vus: 20,
      duration: '2m',
      startTime: '7m30s',
      env: { VOLUME_PHASE: 'extreme' },
      tags: { volume: 'extreme_100000_records' },
    },
  },
  thresholds: {
    http_req_duration:        ['p(95)<5000'],   // Tolerancia mayor por volumen
    http_req_failed:          ['rate<0.10'],    // < 10% errores
    volume_low_latency:       ['p(95)<500'],    // Volumen bajo debe ser rápido
    volume_medium_latency:    ['p(95)<1000'],   // Volumen medio: < 1s
    volume_high_latency:      ['p(95)<2500'],   // Volumen alto: < 2.5s
    volume_extreme_latency:   ['p(95)<5000'],   // Volumen extremo: < 5s
    error_rate:               ['rate<0.10'],
  },
};

// ─── Configuración ──────────────────────────────────────���──────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const HEADERS  = { 'Content-Type': 'application/json', Accept: 'application/json' };

// IDs de préstamos representativos para cada fase de volumen
// (Ajustar según los datos sembrados en la BD)
const VOLUME_CONFIG = {
  low:     { maxLoanId: 50,     description: '~100 préstamos en BD'     },
  medium:  { maxLoanId: 500,    description: '~1.000 préstamos en BD'   },
  high:    { maxLoanId: 5000,   description: '~10.000 préstamos en BD'  },
  extreme: { maxLoanId: 50000,  description: '~100.000 préstamos en BD' },
};

// Función para obtener un ID aleatorio dentro del rango del volumen
function randomId(max) {
  return Math.floor(Math.random() * max) + 1;
}

// ─── Función principal ────────────────────────────────────────────────────
export default function () {
  const phase  = __ENV.VOLUME_PHASE || 'low';
  const config = VOLUME_CONFIG[phase];

  group(`Prueba de Volumen - Fase: ${phase} (${config.description})`, () => {

    // 1. Listar TODOS los préstamos (consulta sin paginación — sensible al volumen)
    group('GET /api/v1/loans/ - Listado completo (sin paginación)', () => {
      const start = Date.now();
      const res   = http.get(`${BASE_URL}/api/v1/loans/`, { headers: HEADERS });
      const elapsed = Date.now() - start;

      const ok = check(res, {
        'status 200':            (r) => r.status === 200,
        'respuesta no vacía':    (r) => r.body && r.body.length > 0,
        'tiempo aceptable':      (r) => r.timings.duration < 5000,
      });

      // Registrar latencia según la fase de volumen
      switch (phase) {
        case 'low':     volumeLowLatency.add(elapsed);     break;
        case 'medium':  volumeMedLatency.add(elapsed);     break;
        case 'high':    volumeHighLatency.add(elapsed);    break;
        case 'extreme': volumeExtremeLatency.add(elapsed); break;
      }

      if (res.status === 500 || res.status === 503) dbErrors.add(1);
      errorRate.add(!ok);
      sleep(0.5);
    });

    // 2. Obtener préstamo por ID (búsqueda directa por PK — debe ser O(1))
    group('GET /api/v1/loans/{id} - Búsqueda por PK', () => {
      const loanId = randomId(config.maxLoanId);
      const res    = http.get(`${BASE_URL}/api/v1/loans/${loanId}`, { headers: HEADERS });

      const ok = check(res, {
        'responde en < 500ms': (r) => r.timings.duration < 500,
        'status válido':       (r) => r.status === 200 || r.status === 404,
      });
      errorRate.add(!ok);
      sleep(0.3);
    });

    // 3. Crear préstamo nuevo (inserción con índices y relaciones)
    group('POST /api/v1/loans/ - Inserción con índices', () => {
      const tomorrow = new Date();
      tomorrow.setDate(tomorrow.getDate() + 7);
      const returnDate = tomorrow.toISOString().split('T')[0];

      const res = http.post(
        `${BASE_URL}/api/v1/loans/`,
        JSON.stringify({
          clientId:         1,
          toolId:           1,
          quantity:         1,
          agreedReturnDate: returnDate,
          notes:            `Volume test fase=${phase} VU=${__VU}`,
        }),
        { headers: HEADERS }
      );

      const ok = check(res, {
        'inserción sin timeout': (r) => r.timings.duration < 5000,
        'sin error 5xx':         (r) => r.status < 500,
      });
      if (res.status === 500 || res.status === 503) dbErrors.add(1);
      errorRate.add(!ok);
      sleep(1);
    });

    // 4. Validar préstamo comprensivo (consulta con JOINs — más costoso con volumen)
    group('POST /api/v1/loans/validate-comprehensive - JOINs bajo volumen', () => {
      const res = http.post(
        `${BASE_URL}/api/v1/loans/validate-comprehensive`,
        JSON.stringify({ clientId: 1, toolId: 1, quantity: 1 }),
        { headers: HEADERS }
      );

      const ok = check(res, {
        'validación < 3000ms': (r) => r.timings.duration < 3000,
        'status válido':       (r) => r.status === 200 || r.status === 400,
      });
      errorRate.add(!ok);
      sleep(0.5);
    });
  });
}

// ─── Resumen con análisis de degradación por volumen ─────────────────────
export function handleSummary(data) {
  const pLow     = data.metrics.volume_low_latency?.values?.['p(95)']     ?? 'N/A';
  const pMed     = data.metrics.volume_medium_latency?.values?.['p(95)']  ?? 'N/A';
  const pHigh    = data.metrics.volume_high_latency?.values?.['p(95)']    ?? 'N/A';
  const pExtreme = data.metrics.volume_extreme_latency?.values?.['p(95)'] ?? 'N/A';

  // Calcular degradación relativa
  const degradationMedVsLow     = typeof pMed  === 'number' && typeof pLow  === 'number' ? ((pMed  - pLow)  / pLow  * 100).toFixed(1) + '%' : 'N/A';
  const degradationHighVsMed    = typeof pHigh === 'number' && typeof pMed  === 'number' ? ((pHigh - pMed)  / pMed  * 100).toFixed(1) + '%' : 'N/A';
  const degradationExtremeVsHigh= typeof pExtreme === 'number' && typeof pHigh === 'number' ? ((pExtreme - pHigh) / pHigh * 100).toFixed(1) + '%' : 'N/A';

  const summary = {
    test_type: 'volume_testing',
    epic:      'Épica 2 - Gestión de Préstamos',
    timestamp: new Date().toISOString(),
    volume_degradation_analysis: {
      phase_low_p95_ms:       pLow,
      phase_medium_p95_ms:    pMed,
      phase_high_p95_ms:      pHigh,
      phase_extreme_p95_ms:   pExtreme,
      degradation_med_vs_low:      degradationMedVsLow,
      degradation_high_vs_med:     degradationHighVsMed,
      degradation_extreme_vs_high: degradationExtremeVsHigh,
      conclusion:
        'Una degradación > 100% entre fases indica ausencia de índices o necesidad de paginación.',
    },
    metrics: {
      http_req_failed_rate:  data.metrics.http_req_failed?.values?.rate,
      http_reqs_total:       data.metrics.http_reqs?.values?.count,
      db_errors:             data.metrics.db_errors?.values?.count,
    },
  };

  return {
    'results/epica2-volume-test-summary.json': JSON.stringify(summary, null, 2),
    stdout: `\n📊 Épica 2 - Prueba de Volumen finalizada\n${JSON.stringify(summary.volume_degradation_analysis, null, 2)}\n`,
  };
}

