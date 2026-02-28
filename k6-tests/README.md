# 🔧 ToolRent — Pruebas de Rendimiento con K6

Pruebas de carga, estrés y volumen para las **Épica 2** (Gestión de Préstamos) y **Épica 6** (Kardex de Movimientos).

---

## 📁 Estructura de archivos

```
k6-tests/
├── config.js                     ← Configuración compartida y utilidades
├── run-tests.ps1                 ← Script para ejecutar todas las pruebas
├── seed-data.sql                 ← Datos de prueba para la BD
├── epica2/
│   ├── load-test.js              ← Carga: 10/50/100/500/1000 VUs concurrentes
│   ├── stress-test.js            ← Estrés: punto de quiebre hasta 1500 VUs
│   └── volume-test.js            ← Volumen: 100/1K/10K/100K préstamos en BD
├── epica6/
│   ├── load-test.js              ← Carga: 10/50/100/500/1000 VUs concurrentes
│   ├── stress-test.js            ← Estrés: punto de quiebre hasta 1200 VUs
│   └── volume-test.js            ← Volumen: 1K/10K/100K/1M movimientos en BD
└── results/                      ← Resultados JSON generados automáticamente
```

---

## ✅ Requisitos previos

### 1. Instalar K6

```powershell
# Con Winget (recomendado en Windows)
winget install k6 --source winget

# Con Chocolatey
choco install k6

# Verificar instalación
k6 version
```

### 2. Backend corriendo
```powershell
# Desde la raíz del proyecto
docker compose up -d
# O bien ejecutar el backend directamente en puerto 8081
```

### 3. Datos de prueba en la BD
```powershell
# Ejecutar el seed de datos antes de las pruebas de volumen
psql -U postgres -d toolrent_db -f k6-tests/seed-data.sql
```

---

## 🚀 Ejecución

### Ejecutar TODAS las pruebas
```powershell
cd k6-tests
.\run-tests.ps1
```

### Ejecutar por épica
```powershell
.\run-tests.ps1 -Epic epica2    # Solo Épica 2
.\run-tests.ps1 -Epic epica6    # Solo Épica 6
```

### Ejecutar por tipo de prueba
```powershell
.\run-tests.ps1 -Type load      # Solo pruebas de carga
.\run-tests.ps1 -Type stress    # Solo pruebas de estrés
.\run-tests.ps1 -Type volume    # Solo pruebas de volumen
```

### Ejecutar script individual (con k6 directamente)
```powershell
# Épica 2 - Carga
k6 run --env BASE_URL=http://localhost:8081 epica2/load-test.js

# Épica 6 - Estrés
k6 run --env BASE_URL=http://localhost:8081 epica6/stress-test.js

# Con salida JSON para análisis posterior
k6 run --out json=results/output.json epica2/load-test.js
```

### Variables de entorno personalizables
```powershell
$env:BASE_URL   = "http://localhost:8081"   # URL del backend
$env:CLIENT_ID  = "1"                        # ID cliente de prueba en BD
$env:TOOL_ID    = "1"                        # ID herramienta de prueba en BD
$env:LOAN_ID    = "1"                        # ID préstamo de prueba en BD
.\run-tests.ps1
```

---

## 📊 Descripción de las pruebas

### Pruebas de Carga (Load Testing)

| Parámetro | Valor |
|-----------|-------|
| Niveles de VUs | 10, 50, 100, 500, 1000 |
| Duración por nivel | 1 minuto |
| Pausa entre niveles | 30 segundos |
| Duración total | ~8 minutos |
| Umbral p(95) | < 2000ms |
| Umbral errores | < 5% |

**Objetivo**: Verificar que el sistema mantiene tiempos de respuesta aceptables bajo carga normal y pico. Cada nivel de VUs simula un escenario de uso distinto.

### Pruebas de Estrés (Stress Testing)

| Parámetro | Épica 2 | Épica 6 |
|-----------|---------|---------|
| VUs máximos | 1500 | 1200 |
| Estrategia | Rampa ascendente | Rampa ascendente |
| Duración total | ~15 minutos | ~15 minutos |

**Objetivo**: Identificar el **punto de quiebre** — el nivel de carga donde el sistema comienza a fallar o degradarse significativamente. Se miden:
- **Latencia p(99)**: si supera 5000ms → degradación severa
- **Tasa de errores**: si supera 20% → punto de quiebre alcanzado  
- **Deadlocks** (Épica 6): errores de contención en escrituras concurrentes

### Pruebas de Volumen (Volume Testing)

| Fase | Épica 2 (préstamos) | Épica 6 (movimientos kardex) |
|------|---------------------|------------------------------|
| Bajo | ~100 registros | ~1.000 movimientos |
| Medio | ~1.000 registros | ~10.000 movimientos |
| Alto | ~10.000 registros | ~100.000 movimientos |
| Extremo | ~100.000 registros | ~1.000.000 movimientos |

**Objetivo**: Evaluar cómo se degrada el rendimiento conforme aumentan los datos en la BD. Una degradación > 100% entre fases indica ausencia de índices.

---

## 📈 Interpretación de resultados

### Métricas principales de K6

| Métrica | Descripción | Umbral aceptable |
|---------|-------------|------------------|
| `http_req_duration p(95)` | 95% de peticiones < X ms | < 2000ms (carga normal) |
| `http_req_duration p(99)` | 99% de peticiones < X ms | < 5000ms (pico) |
| `http_req_failed` | Tasa de peticiones fallidas | < 5% |
| `http_reqs` | Peticiones por segundo (throughput) | Depende del caso |
| `iterations` | Iteraciones completadas | Informativo |
| `vus` | Usuarios virtuales activos | Según escenario |

### Archivos de resultados

Cada prueba genera un archivo JSON en `results/` con el siguiente formato:
```json
{
  "test_type": "load_testing",
  "epic": "Épica 2 - Gestión de Préstamos",
  "timestamp": "2026-02-25T...",
  "metrics": {
    "http_req_duration_p95": 342.5,
    "http_req_duration_p99": 891.2,
    "http_req_failed_rate": 0.002,
    "http_reqs_total": 15420
  }
}
```

### Cómo identificar el punto de quiebre (estrés)

1. **En la consola de K6**: observa el gráfico de VUs vs latencia en tiempo real
2. **En el JSON de resultados**: busca el campo `breaking_point_analysis`
3. **Indicadores visuales**:
   - Latencia p(99) que salta bruscamente (×5 o más)
   - `http_req_failed` que supera 20%
   - Throughput (req/s) que deja de crecer o decrece

### Cómo interpretar pruebas de volumen

Calcula la **degradación relativa** entre fases:
```
Degradación (%) = ((p95_fase_N - p95_fase_N-1) / p95_fase_N-1) × 100
```

| Degradación | Interpretación |
|-------------|----------------|
| < 20% | ✅ Índices funcionando correctamente |
| 20% - 100% | ⚠️ Degrada pero tolerable |
| > 100% | ❌ Faltan índices o se necesita paginación |
| Timeout/0ms | 🔴 La consulta no termina — tabla demasiado grande |

---

## 🛠️ Solución de problemas

### "k6 no está instalado"
```powershell
winget install k6 --source winget
# Reiniciar PowerShell después de instalar
```

### "Cannot connect to backend"
```powershell
# Verificar que el backend esté corriendo
docker compose ps
# O revisar logs
docker compose logs backend
```

### "Prueba de estrés falla desde el inicio"
- Asegúrate de que la BD tenga los datos del seed
- Aumenta el `LOAN_ID` si el préstamo ID=1 ya fue devuelto

### Resultados con muchos errores 400 en Kardex
- Algunos errores 400 son esperados (stock insuficiente, herramienta no disponible)
- Los scripts los consideran como respuestas válidas del sistema
- Solo los errores 500/503 indican problemas reales del servidor

---

## 📋 Checklist para la entrega

- [ ] K6 instalado y funcionando (`k6 version`)
- [ ] Backend corriendo en localhost:8081
- [ ] Seed de datos ejecutado (`seed-data.sql`)
- [ ] Prueba de carga Épica 2 ejecutada → `results/epica2-load-*.json`
- [ ] Prueba de estrés Épica 2 ejecutada → `results/epica2-stress-*.json`
- [ ] Prueba de volumen Épica 2 ejecutada → `results/epica2-volume-*.json`
- [ ] Prueba de carga Épica 6 ejecutada → `results/epica6-load-*.json`
- [ ] Prueba de estrés Épica 6 ejecutada → `results/epica6-stress-*.json`
- [ ] Prueba de volumen Épica 6 ejecutada → `results/epica6-volume-*.json`
- [ ] Resultados interpretados y documentados

