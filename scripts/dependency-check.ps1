# ============================================================
# dependency-check.ps1
# Ejecuta el chequeo de dependencias OWASP localmente.
#
# Uso:
#   .\scripts\dependency-check.ps1              → actualiza la DB NVD y analiza
#   .\scripts\dependency-check.ps1 -NoUpdate    → usa la DB cacheada (más rápido)
#   .\scripts\dependency-check.ps1 -ApiKey TU_KEY
# ============================================================

param(
    [switch]$NoUpdate,
    [string]$ApiKey = $env:NVD_API_KEY
)

$ErrorActionPreference = "Stop"

$mvnArgs = @(
    "org.owasp:dependency-check-maven:aggregate",
    "-Dformat=HTML",
    "-DsuppressionFiles=dependency-check-suppressions.xml",
    "-DfailBuildOnCVSS=1"
)

if ($NoUpdate) {
    Write-Host "⚡ Modo sin actualización: usando base de datos NVD cacheada." -ForegroundColor Cyan
    $mvnArgs += "-DautoUpdate=false"
} else {
    Write-Host "🔄 Actualizando base de datos NVD (puede tardar la primera vez)..." -ForegroundColor Yellow
}

if ($ApiKey) {
    Write-Host "🔑 Usando NVD API Key." -ForegroundColor Green
    $mvnArgs += "-DnvdApiKey=$ApiKey"
} else {
    Write-Host "⚠️  Sin NVD API Key — se aplicará rate-limiting. Considera obtener una gratis en:" -ForegroundColor Yellow
    Write-Host "   https://nvd.nist.gov/developers/request-an-api-key" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "▶ mvn $($mvnArgs -join ' ')" -ForegroundColor DarkGray
Write-Host ""

mvn @mvnArgs

if ($LASTEXITCODE -eq 0) {
    Write-Host ""
    Write-Host "✅ Dependency Check completado. Reporte en: target/dependency-check-report.html" -ForegroundColor Green
    # Abrir el reporte en el navegador
    $report = Join-Path $PSScriptRoot "..\target\dependency-check-report.html"
    if (Test-Path $report) {
        Start-Process $report
    }
} else {
    Write-Host ""
    Write-Host "❌ Se encontraron vulnerabilidades. Revisa el reporte en: target/dependency-check-report.html" -ForegroundColor Red
    exit $LASTEXITCODE
}
