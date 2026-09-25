#!/usr/bin/env bash
# Initialise SonarQube (idempotent) : mot de passe admin, projet kfokam48-presence, token d'analyse.
# Usage : bash infra/sonar/init.sh   (lit .env à la racine s'il existe)
# Variables : SONAR_ADMIN_PASSWORD (obligatoire), SONAR_HOST_URL (défaut http://localhost:9000), SONAR_TOKEN (facultatif)
set -euo pipefail

RACINE="$(cd "$(dirname "$0")/../.." && pwd)"
if [[ -f "$RACINE/.env" ]]; then
  set -a; source "$RACINE/.env"; set +a
fi

HOTE="${SONAR_HOST_URL:-http://localhost:9000}"
PROJET="kfokam48-presence"
: "${SONAR_ADMIN_PASSWORD:?Définir SONAR_ADMIN_PASSWORD dans .env (12 caractères min., majuscule, minuscule, chiffre, caractère spécial)}"

api() { curl -sS -f "$@"; }

echo "Attente de SonarQube sur $HOTE…"
for i in $(seq 1 60); do
  if curl -s "$HOTE/api/system/status" | grep -q '"status":"UP"'; then break; fi
  [[ $i == 60 ]] && { echo "❌ SonarQube n'est pas UP après 5 min"; exit 1; }
  sleep 5
done
echo "✅ SonarQube UP"

# 1. Mot de passe administrateur (seulement s'il vaut encore admin/admin)
if curl -s -u "admin:$SONAR_ADMIN_PASSWORD" "$HOTE/api/authentication/validate" | grep -q '"valid":true'; then
  echo "✅ Mot de passe admin déjà défini"
else
  api -u admin:admin -X POST "$HOTE/api/users/change_password" \
    --data-urlencode "login=admin" --data-urlencode "previousPassword=admin" \
    --data-urlencode "password=$SONAR_ADMIN_PASSWORD" >/dev/null \
    || { echo "❌ Impossible de changer le mot de passe admin (mot de passe trop faible ou déjà modifié autrement)"; exit 1; }
  echo "✅ Mot de passe admin changé"
fi
ADMIN=(-u "admin:$SONAR_ADMIN_PASSWORD")

# 2. Projet
if api "${ADMIN[@]}" "$HOTE/api/projects/search?projects=$PROJET" | grep -q "\"key\":\"$PROJET\""; then
  echo "✅ Projet $PROJET déjà créé"
else
  api "${ADMIN[@]}" -X POST "$HOTE/api/projects/create" \
    --data-urlencode "project=$PROJET" --data-urlencode "name=KFOKAM48 Présence" >/dev/null
  echo "✅ Projet $PROJET créé"
fi

# 3. Token d'analyse (seulement si SONAR_TOKEN est absent ou invalide) : affiché une seule fois
if [[ -n "${SONAR_TOKEN:-}" ]] && curl -s -u "$SONAR_TOKEN:" "$HOTE/api/authentication/validate" | grep -q '"valid":true'; then
  echo "✅ SONAR_TOKEN valide, aucun nouveau token"
else
  NOM="analyse-$(date +%Y%m%d-%H%M%S)"
  REPONSE=$(api "${ADMIN[@]}" -X POST "$HOTE/api/user_tokens/generate" \
    --data-urlencode "name=$NOM" --data-urlencode "type=PROJECT_ANALYSIS_TOKEN" --data-urlencode "projectKey=$PROJET")
  TOKEN=$(printf '%s' "$REPONSE" | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')
  [[ -n "$TOKEN" ]] || { echo "❌ Génération du token impossible : $REPONSE"; exit 1; }
  echo "🔑 Nouveau token ($NOM), affiché une seule fois — à copier dans .env (jamais dans Git) :"
  echo "SONAR_TOKEN=$TOKEN"
fi

echo "Analyse : cd backend && ./mvnw verify sonar:sonar -Dsonar.host.url=$HOTE -Dsonar.token=\$SONAR_TOKEN -Dsonar.qualitygate.wait=true"
