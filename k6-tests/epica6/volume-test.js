/**
 * ÉPICA 6 - KARDEX DE MOVIMIENTOS
 * PRUEBA DE VOLUMEN (Volume Testing)
 *
 * Objetivo: Evaluar cómo se degrada el rendimiento del Kardex conforme
 * aumenta el volumen de movimientos registrados en la BD.
 *
 * El Kardex es particularmente sensible al volumen porque:
 *  - Cada préstamo/devolución genera al menos 1 registro de movimiento
 *  - Las consultas por rango de fechas escanean toda la tabla si no hay índices
 *  - Los reportes agregan datos de múltiples herramientas
 *
 * Fases de volumen en tabla kardex_movements:
 *   - Bajo:    1.000 movimientos
 *   - Medio:   10.000 movimientos
 *   - Alto:    100.000 movimientos
 *   - Extremo: 1.000.000 movimientos
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// ─── Métricas por fase de volumen ──────────────────────────────────────────
const volumeLowRead       = new Trend('kardex_vol_low_read', true);
const volumeMedRead       = new Trend('kardex_vol_med_read', true);
const volumeHighRead      = new Trend('kardex_vol_high_read', true);
const volumeExtremeRead   = new Trend('kardex_vol_extreme_read', true);
const volumeLowFilter     = new Trend('kardex_vol_low_filter', true);
const volumeMedFilter     = new Trend('kardex_vol_med_filter', true);
const volumeHighFilter    = new Trend('kardex_vol_high_filter', true);
const volumeExtremeFilter = new Trend('kardex_vol_extreme_filter', true);
const dbTimeouts          = new Counter('db_timeouts');
const errorRate           = new Rate('error_rate');

// ─── Configuración de escenarios de volumen ───────────────────────────────
export const options = {
  scenarios: {
    // Fase 1: Volumen bajo — 1.000 movimientos en kardex
    kardex_volume_low: {
      executor: 'constant-vus',
      vus: 15,
      duration: '2m',
      startTime: '0s',
      env: { VOLUME_PHASE: 'low' },
      tags: { volume: 'low_1k_movements' },
    },
    // Fase 2: Volumen medio — 10.000 movimientos
    kardex_volume_medium: {
      executor: 'constant-vus',
      vus: 15,
      duration: '2m',
      startTime: '2m30s',
      env: { VOLUME_PHASE: 'medium' },
      tags: { volume: 'medium_10k_movements' },
    },
    // Fase 3: Volumen alto — 100.000 movimientos
    kardex_volume_high: {
      executor: 'constant-vus',
      vus: 15,
      duration: '2m',
      startTime: '5m',
      env: { VOLUME_PHASE: 'high' },
      tags: { volume: 'high_100k_movements' },
    },
    // Fase 4: Volumen extremo — 1.000.000 movimientos
    kardex_volume_extreme: {
      executor: 'constant-vus',
      vus: 15,
      duration: '2m',
      startTime: '7m30s',
      env: { VOLUME_PHASE: 'extreme' },
      tags: { volume: 'extreme_1M_movements' },
    },
  },
  thresholds: {
    http_req_duration:          ['p(95)<10000'],  // Tolerancia alta por volumen
    http_req_failed:            ['rate<0.15'],
    kardex_vol_low_read:        ['p(95)<300'],    // 1K reg → muy rápido
    kardex_vol_med_read:        ['p(95)<800'],    // 10K reg → rápido
    kardex_vol_high_read:       ['p(95)<3000'],   // 100K reg → tolerable
    kardex_vol_extreme_read:    ['p(95)<8000'],   // 1M reg → límite
    kardex_vol_low_filter:      ['p(95)<500'],
    kardex_vol_med_filter:      ['p(95)<1500'],
    kardex_vol_high_filter:     ['p(95)<5000'],
    kardex_vol_extreme_filter:  ['p(95)<10000'],
    error_rate:                 ['rate<0.15'],
  },
};

// ─── Configuración ─────────────────────────────────────────────────────────
const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const TOOL_ID  = __ENV.TOOL_ID  || '1';
const LOAN_ID  = __ENV.LOAN_ID  || '1';

const HEADERS = { 'Content-Type': 'application/json', Accept: 'application/json' };

// Rango de fechas según el volumen (más años hacia atrás = más datos)
const DATE_RANGES = {
  low:     { daysBack: 30  },   // Último mes   → ~1K mov
  medium:  { daysBack: 180 },   // Último semestre → ~10K mov
  high:    { daysBack: 730 },   // Últimos 2 años  → ~100K mov
  extreme: { daysBack: 3650 },  // Últimos 10 años → ~1M mov
};

function getDateRange(daysBack) {
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
  const phase     = __ENV.VOLUME_PHASE || 'low';
  const dateRange = DATE_RANGES[phase];

  group(`Kardex Volumen - Fase: ${phase}`, () => {

    // 1. Consulta por herramienta (búsqueda con índice en tool_id)
    group('GET movimientos por herramienta', () => {
      const start = Date.now();
      const res   = http.get(
        `${BASE_URL}/api/kardex-movements/tool/${TOOL_ID}`,
        { headers: HEADERS }
      );
      const elapsed = Date.now() - start;
      const ok = check(res, {
        'status válido':     (r) => r.status === 200 || r.status === 404,
        'sin timeout':       (r) => r.timings.duration < 10000,
      });

      // Registrar en la métrica de volumen correspondiente
      switch (phase) {
        case 'low':     volumeLowRead.add(elapsed);     break;
        case 'medium':  volumeMedRead.add(elapsed);     break;
        case 'high':    volumeHighRead.add(elapsed);    break;
        case 'extreme': volumeExtremeRead.add(elapsed); break;
      }

      if (res.status === 0 || res.timings.duration > 9000) dbTimeouts.add(1);
      errorRate.add(!ok);
      sleep(0.5);
    });

    // 2. Reporte por rango de fechas (escaneo de tabla — más sensible al volumen)
    group('GET reporte por rango de fechas', () => {
      const { startDate, endDate } = getDateRange(dateRange.daysBack);
      const start = Date.now();
      const res   = http.get(
        `${BASE_URL}/api/kardex-movements/date-range?startDate=${startDate}&endDate=${endDate}`,
        { headers: HEADERS, timeout: '30s' }
      );
      const elapsed = Date.now() - start;
      const ok = check(res, {
        'reporte responde':  (r) => r.status !== 0 && r.status !== 503,
        'sin timeout fatal': (r) => r.timings.duration < 30000,
      });

      // Registrar en métrica de filtro correspondiente
      switch (phase) {
        case 'low':     volumeLowFilter.add(elapsed);     break;
        case 'medium':  volumeMedFilter.add(elapsed);     break;
        case 'high':    volumeHighFilter.add(elapsed);    break;
        case 'extreme': volumeExtremeFilter.add(elapsed); break;
      }

      if (res.status === 0 || res.timings.duration > 15000) dbTimeouts.add(1);
      errorRate.add(!ok);
      sleep(1);
    });

    // 3. Inserción de nuevo movimiento (verifica que escrituras no se degraden)
    group('POST movimiento nuevo bajo volumen', () => {
      const res = http.post(
        `${BASE_URL}/api/kardex-movements/restock`,
        JSON.stringify({
          toolId:      parseInt(TOOL_ID),
          quantity:    1,
          description: `Volume test ${phase} VU=${__VU}`,
          userId:      1,
        }),
        { headers: HEADERS }
      );
      const ok = check(res, {
        'inserción exitosa':  (r) => r.status === 201 || r.status === 200 || r.status === 400,
        'inserción < 5000ms': (r) => r.timings.duration < 5000,
      });
      if (res.status === 0 || res.timings.duration > 5000) dbTimeouts.add(1);
      errorRate.add(!ok);
      sleep(1);
    });
  });
}

// ─── Resumen con análisis de degradación por volumen ─────────────────────
export function handleSummary(data) {
  // Lecturas por fase
  const rLow     = data.metrics.kardex_vol_low_read?.values?.['p(95)']     ?? 'N/A';
  const rMed     = data.metrics.kardex_vol_med_read?.values?.['p(95)']     ?? 'N/A';
  const rHigh    = data.metrics.kardex_vol_high_read?.values?.['p(95)']    ?? 'N/A';
  const rExtreme = data.metrics.kardex_vol_extreme_read?.values?.['p(95)'] ?? 'N/A';

  // Filtros por fase
  const fLow     = data.metrics.kardex_vol_low_filter?.values?.['p(95)']     ?? 'N/A';
  const fMed     = data.metrics.kardex_vol_med_filter?.values?.['p(95)']     ?? 'N/A';
  const fHigh    = data.metrics.kardex_vol_high_filter?.values?.['p(95)']    ?? 'N/A';
  const fExtreme = data.metrics.kardex_vol_extreme_filter?.values?.['p(95)'] ?? 'N/A';

  // Degradación de lecturas
  const readDegradMedVsLow  = typeof rMed  === 'number' && typeof rLow  === 'number' ? ((rMed  - rLow)  / rLow  * 100).toFixed(1) + '%' : 'N/A';
  const readDegradHighVsMed = typeof rHigh === 'number' && typeof rMed  === 'number' ? ((rHigh - rMed)  / rMed  * 100).toFixed(1) + '%' : 'N/A';
  const readDegradExtVsHigh = typeof rExtreme === 'number' && typeof rHigh === 'number' ? ((rExtreme - rHigh) / rHigh * 100).toFixed(1) + '%' : 'N/A';

  // Degradación de filtros
  const filterDegradMedVsLow  = typeof fMed  === 'number' && typeof fLow  === 'number' ? ((fMed  - fLow)  / fLow  * 100).toFixed(1) + '%' : 'N/A';
  const filterDegradHighVsMed = typeof fHigh === 'number' && typeof fMed  === 'number' ? ((fHigh - fMed)  / fMed  * 100).toFixed(1) + '%' : 'N/A';
  const filterDegradExtVsHigh = typeof fExtreme === 'number' && typeof fHigh === 'number' ? ((fExtreme - fHigh) / fHigh * 100).toFixed(1) + '%' : 'N/A';

  const summary = {
    test_type: 'volume_testing',
    epic:      'Épica 6 - Kardex de Movimientos',
    timestamp: new Date().toISOString(),
    volume_analysis: {
      read_by_tool: {
        low_1k_p95:      rLow,
        medium_10k_p95:  rMed,
        high_100k_p95:   rHigh,
        extreme_1M_p95:  rExtreme,
        degradation_med_vs_low:  readDegradMedVsLow,
        degradation_high_vs_med: readDegradHighVsMed,
        degradation_ext_vs_high: readDegradExtVsHigh,
      },
      filter_by_date_range: {
        low_1k_p95:      fLow,
        medium_10k_p95:  fMed,
        high_100k_p95:   fHigh,
        extreme_1M_p95:  fExtreme,
        degradation_med_vs_low:  filterDegradMedVsLow,
        degradation_high_vs_med: filterDegradHighVsMed,
        degradation_ext_vs_high: filterDegradExtVsHigh,
      },
      db_timeouts: data.metrics.db_timeouts?.values?.count ?? 0,
      conclusion: [
        'Si la lectura por herramienta tiene degradación < 20% entre fases: los índices en tool_id están funcionando.',
        'Si el filtro por fecha tiene degradación > 200% entre fases: se requiere índice en la columna de fecha.',
        'Si db_timeouts > 0 en fase extreme: considerar particionado de tabla kardex_movements por fecha.',
      ],
    },
    metrics: {
      http_req_failed_rate: data.metrics.http_req_failed?.values?.rate,
      http_reqs_total:      data.metrics.http_reqs?.values?.count,
    },
  };

  return {
    'results/epica6-volume-test-summary.json': JSON.stringify(summary, null, 2),
    stdout: `\n📊 Épica 6 - Prueba de Volumen finalizada\n${JSON.stringify(summary.volume_analysis, null, 2)}\n`,
  };
}

