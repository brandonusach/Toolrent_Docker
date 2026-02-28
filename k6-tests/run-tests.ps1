<#
.SYNOPSIS
    Ejecuta las pruebas de rendimiento K6 para ToolRent.

.DESCRIPTION
    Script para ejecutar pruebas de carga, estrés y volumen
    de las Épicas 2 (Préstamos) y 6 (Kardex).

.PARAMETER Epic
    Filtrar por épica: epica2, epica6. Si no se especifica, ejecuta ambas.

.PARAMETER Type
    Filtrar por tipo de prueba: load, stress, volume. Si no se especifica, ejecuta todos.

.PARAMETER BaseUrl
    URL base del backend. Por defecto: http://localhost:8081

.EXAMPLE
    .\run-tests.ps1
    .\run-tests.ps1 -Epic epica2
    .\run-tests.ps1 -Type load
    .\run-tests.ps1 -Epic epica6 -Type stress
#>

param(
    [ValidateSet("epica2", "epica6", "")]
    [string]$Epic = "",

    [ValidateSet("load", "stress", "volume", "")]
    [string]$Type = "",

    [string]$BaseUrl = "http://localhost:8081"
)

# ─── Configuración ──────────────────────────────────────────────────────────
$ErrorActionPreference = "Continue"
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ResultsDir = Join-Path $ScriptDir "results"

# Crear carpeta de resultados si no existe
if (-not (Test-Path $ResultsDir)) {
    New-Item -ItemType Directory -Path $ResultsDir -Force | Out-Null
}

# ─── Verificar que K6 está instalado ────────────────────────────────────────
try {
    $k6Version = & k6 version 2>&1
    Write-Host ""
    Write-Host "  K6 detectado: $k6Version" -ForegroundColor Green
} catch {
    Write-Host ""
    Write-Host "  ERROR: K6 no está instalado." -ForegroundColor Red
    Write-Host "  Instálalo con: winget install k6 --source winget" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

# ─── Definir matriz de pruebas ──────────────────────────────────────────────
$allTests = @(
    @{ Epic = "epica2"; Type = "load";    File = "epica2/load-test.js";    Name = "Épica 2 - Carga" },
    @{ Epic = "epica2"; Type = "stress";  File = "epica2/stress-test.js";  Name = "Épica 2 - Estrés" },
    @{ Epic = "epica2"; Type = "volume";  File = "epica2/volume-test.js";  Name = "Épica 2 - Volumen" },
    @{ Epic = "epica6"; Type = "load";    File = "epica6/load-test.js";    Name = "Épica 6 - Carga" },
    @{ Epic = "epica6"; Type = "stress";  File = "epica6/stress-test.js";  Name = "Épica 6 - Estrés" },
    @{ Epic = "epica6"; Type = "volume";  File = "epica6/volume-test.js";  Name = "Épica 6 - Volumen" }
)

# ─── Filtrar según parámetros ───────────────────────────────────────────────
$testsToRun = $allTests

if ($Epic -ne "") {
    $testsToRun = $testsToRun | Where-Object { $_.Epic -eq $Epic }
}

if ($Type -ne "") {
    $testsToRun = $testsToRun | Where-Object { $_.Type -eq $Type }
}

if ($testsToRun.Count -eq 0) {
    Write-Host ""
    Write-Host "  No se encontraron pruebas que coincidan con los filtros." -ForegroundColor Yellow
    Write-Host "  Epic: '$Epic'  Type: '$Type'" -ForegroundColor Yellow
    Write-Host ""
    exit 1
}

# ─── Banner de inicio ───────────────────────────────────────────────────────
Write-Host ""
Write-Host "  ══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  🔧 ToolRent — Pruebas de Rendimiento K6" -ForegroundColor Cyan
Write-Host "  ══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Backend URL:    $BaseUrl" -ForegroundColor White
Write-Host "  Filtro Épica:   $(if ($Epic) { $Epic } else { 'Todas' })" -ForegroundColor White
Write-Host "  Filtro Tipo:    $(if ($Type) { $Type } else { 'Todos' })" -ForegroundColor White
Write-Host "  Pruebas a ejecutar: $($testsToRun.Count)" -ForegroundColor White
Write-Host "  Resultados en:  $ResultsDir" -ForegroundColor White
Write-Host ""

# ─── Ejecutar pruebas ───────────────────────────────────────────────────────
$passed = 0
$failed = 0
$startTime = Get-Date

foreach ($test in $testsToRun) {
    $testFile = Join-Path $ScriptDir $test.File
    $testName = $test.Name
    $jsonOutput = Join-Path $ResultsDir "$($test.Epic)-$($test.Type)-test-output.json"

    Write-Host "  ──────────────────────────────────────────────────────" -ForegroundColor DarkGray
    Write-Host "  ▶ Ejecutando: $testName" -ForegroundColor Yellow
    Write-Host "    Archivo:    $($test.File)" -ForegroundColor DarkGray
    Write-Host ""

    $testStart = Get-Date

    # Ejecutar K6
    & k6 run `
        --env BASE_URL=$BaseUrl `
        --env CLIENT_ID=1 `
        --env TOOL_ID=1 `
        --env LOAN_ID=1 `
        --out "json=$jsonOutput" `
        $testFile

    $exitCode = $LASTEXITCODE
    $testDuration = (Get-Date) - $testStart

    if ($exitCode -eq 0) {
        Write-Host ""
        Write-Host "  ✅ $testName — PASÓ ($('{0:mm\:ss}' -f $testDuration))" -ForegroundColor Green
        $passed++
    } else {
        Write-Host ""
        Write-Host "  ❌ $testName — FALLÓ (exit code: $exitCode) ($('{0:mm\:ss}' -f $testDuration))" -ForegroundColor Red
        $failed++
    }
    Write-Host ""
}

# ─── Resumen final ──────────────────────────────────────────────────────────
$totalDuration = (Get-Date) - $startTime

Write-Host "  ══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host "  📊 RESUMEN DE EJECUCIÓN" -ForegroundColor Cyan
Write-Host "  ══════════════════════════════════════════════════════════" -ForegroundColor Cyan
Write-Host ""
Write-Host "  Total de pruebas:  $($testsToRun.Count)" -ForegroundColor White
Write-Host "  ✅ Pasaron:        $passed" -ForegroundColor Green
Write-Host "  ❌ Fallaron:       $failed" -ForegroundColor $(if ($failed -gt 0) { "Red" } else { "Green" })
Write-Host "  ⏱️  Duración total: $('{0:hh\:mm\:ss}' -f $totalDuration)" -ForegroundColor White
Write-Host ""

if ($failed -eq 0) {
    Write-Host "  🎉 ¡Todas las pruebas pasaron exitosamente!" -ForegroundColor Green
} else {
    Write-Host "  ⚠️  Algunas pruebas fallaron. Revisa los resultados en:" -ForegroundColor Yellow
    Write-Host "     $ResultsDir" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "  Archivos de resultados generados:" -ForegroundColor DarkGray
Get-ChildItem $ResultsDir -Filter "*.json" | ForEach-Object {
    Write-Host "    📄 $($_.Name) ($('{0:N1}' -f ($_.Length / 1KB)) KB)" -ForegroundColor DarkGray
}
Write-Host ""

exit $failed

