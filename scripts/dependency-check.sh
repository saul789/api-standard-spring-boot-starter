#!/usr/bin/env bash
# ============================================================
# dependency-check.sh
# Ejecuta el chequeo de dependencias OWASP localmente.
#
# Uso:
#   ./scripts/dependency-check.sh              → actualiza la DB NVD y analiza
#   ./scripts/dependency-check.sh --no-update  → usa la DB cacheada (más rápido)
#   NVD_API_KEY=tu_key ./scripts/dependency-check.sh
# ============================================================

set -e

NO_UPDATE=false

for arg in "$@"; do
  case $arg in
    --no-update) NO_UPDATE=true ;;
  esac
done

MVN_ARGS=(
  "org.owasp:dependency-check-maven:aggregate"
  "-Dformat=HTML"
  "-DsuppressionFiles=dependency-check-suppressions.xml"
  "-DfailBuildOnCVSS=1"
)

if [ "$NO_UPDATE" = true ]; then
  echo "⚡ Modo sin actualización: usando base de datos NVD cacheada."
  MVN_ARGS+=("-DautoUpdate=false")
else
  echo "🔄 Actualizando base de datos NVD (puede tardar la primera vez)..."
fi

if [ -n "$NVD_API_KEY" ]; then
  echo "🔑 Usando NVD API Key."
  MVN_ARGS+=("-DnvdApiKey=$NVD_API_KEY")
else
  echo "⚠️  Sin NVD API Key — se aplicará rate-limiting. Considera obtener una gratis en:"
  echo "   https://nvd.nist.gov/developers/request-an-api-key"
fi

echo ""
echo "▶ mvn ${MVN_ARGS[*]}"
echo ""

mvn "${MVN_ARGS[@]}"

echo ""
echo "✅ Dependency Check completado. Reporte en: target/dependency-check-report.html"

# Intentar abrir el reporte automáticamente
REPORT="$(dirname "$0")/../target/dependency-check-report.html"
if [ -f "$REPORT" ]; then
  case "$(uname -s)" in
    Darwin) open "$REPORT" ;;
    Linux)  xdg-open "$REPORT" 2>/dev/null || true ;;
  esac
fi
