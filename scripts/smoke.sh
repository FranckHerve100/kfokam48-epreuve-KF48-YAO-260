#!/usr/bin/env bash
# Tests de fumée et de non-régression contre l'application démarrée (docker compose up).
# Usage : bash scripts/smoke.sh [URL]   (défaut : http://localhost:8080 ou $BASE_URL)
# Sort en erreur au premier échec. Dépendances : bash, curl.
set -euo pipefail

BASE_URL="${1:-${BASE_URL:-http://localhost:8080}}"
ATTENTE_MAX="${ATTENTE_MAX:-60}"
TOTAL=0

echec() {
  echo "❌ $*" >&2
  exit 1
}

# verifier <description> <méthode> <chemin> <statut attendu> [motif attendu dans le corps] [corps JSON]
verifier() {
  local description="$1" methode="$2" chemin="$3" attendu="$4" motif="${5:-}" donnees="${6:-}"
  local reponse statut corps
  if [[ -n "$donnees" ]]; then
    reponse=$(curl -sS -L -X "$methode" -H 'Content-Type: application/json' -d "$donnees" -w '\n%{http_code}' "$BASE_URL$chemin") \
      || echec "$description : $BASE_URL$chemin injoignable"
  else
    reponse=$(curl -sS -L -X "$methode" -w '\n%{http_code}' "$BASE_URL$chemin") \
      || echec "$description : $BASE_URL$chemin injoignable"
  fi
  statut="${reponse##*$'\n'}"
  corps="${reponse%$'\n'*}"
  [[ "$statut" == "$attendu" ]] || echec "$description : $methode $chemin → $statut (attendu $attendu)
$corps"
  if [[ -n "$motif" && "$corps" != *"$motif"* ]]; then
    echec "$description : $methode $chemin → corps sans « $motif »
$corps"
  fi
  TOTAL=$((TOTAL + 1))
  echo "✅ $description ($methode $chemin → $statut)"
}

echo "Tests de fumée sur $BASE_URL"

# ── 00 Santé ────────────────────────────────────────────────────────────────
for ((i = 1; i <= ATTENTE_MAX; i++)); do
  if curl -sf "$BASE_URL/actuator/health" 2>/dev/null | grep -q '"status":"UP"'; then
    break
  fi
  ((i == ATTENTE_MAX)) && echec "l'application n'est pas UP après ${ATTENTE_MAX} s"
  sleep 1
done
verifier "Santé UP"                   GET /actuator/health           200 '"status":"UP"'
verifier "Sonde readiness"            GET /actuator/health/readiness 200 '"status":"UP"'
verifier "Version et commit"          GET /actuator/info             200 '"version":"'
verifier "Métriques Prometheus"       GET /actuator/prometheus       200 'jvm_memory_used_bytes'
verifier "Contrat imposé servi"       GET /contrat.yaml              200 'openapi: 3.0.3'
verifier "Implémentation (springdoc)" GET /v3/api-docs               200 '"openapi"'
verifier "Swagger UI"                 GET /swagger-ui.html           200 'swagger-ui'
verifier "Erreur au format du contrat" GET /api/inexistant           404 '"code":"RESSOURCE_INCONNUE"'

echo "🎉 $TOTAL vérifications réussies"
