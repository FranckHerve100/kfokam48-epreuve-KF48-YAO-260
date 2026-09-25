#!/usr/bin/env bash
# Initialise Nexus (idempotent) via l'API REST : mot de passe admin, licence Community Edition,
# lecture anonyme, Docker Bearer Token Realm, dépôts docker-proxy (8082) et docker-hosted (8083).
# Usage : bash infra/nexus/init.sh   (lit .env à la racine s'il existe)
# Variables : NEXUS_ADMIN_PASSWORD (obligatoire), NEXUS_EULA_ACCEPTEE=true (obligatoire, décision humaine),
#             NEXUS_URL (défaut http://localhost:8081)
set -euo pipefail

RACINE="$(cd "$(dirname "$0")/../.." && pwd)"
if [[ -f "$RACINE/.env" ]]; then
  set -a; source "$RACINE/.env"; set +a
fi

HOTE="${NEXUS_URL:-http://localhost:8081}"
API="$HOTE/service/rest/v1"
COMPOSE=(docker compose -f "$RACINE/infra/docker-compose.infra.yml")
: "${NEXUS_ADMIN_PASSWORD:?Définir NEXUS_ADMIN_PASSWORD dans .env}"

echo "Attente de Nexus sur $HOTE…"
for i in $(seq 1 60); do
  curl -sf "$API/status/writable" >/dev/null && break
  [[ $i == 60 ]] && { echo "❌ Nexus n'est pas prêt après 5 min"; exit 1; }
  sleep 5
done
echo "✅ Nexus prêt"

# 1. Mot de passe administrateur : le mot de passe initial est dans /nexus-data/admin.password
if curl -sf -u "admin:$NEXUS_ADMIN_PASSWORD" "$API/status/check" >/dev/null; then
  echo "✅ Mot de passe admin déjà défini"
else
  INITIAL=$("${COMPOSE[@]}" exec -T nexus cat /nexus-data/admin.password 2>/dev/null) \
    || { echo "❌ Mot de passe initial introuvable et NEXUS_ADMIN_PASSWORD refusé"; exit 1; }
  curl -sf -u "admin:$INITIAL" -X PUT -H 'Content-Type: text/plain' \
    --data "$NEXUS_ADMIN_PASSWORD" "$API/security/users/admin/change-password"
  echo "✅ Mot de passe admin changé"
fi
ADMIN=(-sf -u "admin:$NEXUS_ADMIN_PASSWORD")

# 2. Licence Community Edition : sans elle, les dépôts proxy renvoient 403
EULA=$(curl "${ADMIN[@]}" "$API/system/eula")
if grep -q '"accepted" *: *true' <<<"$EULA"; then
  echo "✅ Licence déjà acceptée"
elif [[ "${NEXUS_EULA_ACCEPTEE:-}" == "true" ]]; then
  sed 's/"accepted" *: *false/"accepted" : true/' <<<"$EULA" \
    | curl "${ADMIN[@]}" -X POST -H 'Content-Type: application/json' --data @- "$API/system/eula"
  echo "✅ Licence acceptée (NEXUS_EULA_ACCEPTEE=true)"
else
  echo "❌ Licence non acceptée : lire https://links.sonatype.com/products/nxrm/ce-eula puis mettre NEXUS_EULA_ACCEPTEE=true dans .env"
  exit 1
fi

# 3. Lecture anonyme (docker pull et téléchargements Maven sans identifiants)
curl "${ADMIN[@]}" -X PUT -H 'Content-Type: application/json' "$API/security/anonymous" \
  --data '{"enabled":true,"userId":"anonymous","realmName":"NexusAuthorizingRealm"}' >/dev/null
echo "✅ Accès anonyme en lecture"

# 4. Docker Bearer Token Realm (indispensable au pull anonyme via le client Docker)
ACTIFS=$(curl "${ADMIN[@]}" "$API/security/realms/active")
if grep -q '"DockerToken"' <<<"$ACTIFS"; then
  echo "✅ Docker Bearer Token Realm déjà actif"
else
  NOUVEAUX=$(sed 's/]$/,"DockerToken"]/; s/\[,/[/' <<<"$(tr -d ' \n' <<<"$ACTIFS")")
  curl "${ADMIN[@]}" -X PUT -H 'Content-Type: application/json' --data "$NOUVEAUX" "$API/security/realms/active"
  echo "✅ Docker Bearer Token Realm activé"
fi

# 5. Dépôts Docker
creer_depot() { # nom type json
  if curl "${ADMIN[@]}" -o /dev/null "$API/repositories/docker/$2/$1"; then
    echo "✅ Dépôt $1 déjà créé"
  else
    curl "${ADMIN[@]}" -X POST -H 'Content-Type: application/json' --data "$3" "$API/repositories/docker/$2"
    echo "✅ Dépôt $1 créé"
  fi
}
creer_depot docker-proxy proxy '{
  "name": "docker-proxy", "online": true,
  "storage": {"blobStoreName": "default", "strictContentTypeValidation": true},
  "proxy": {"remoteUrl": "https://registry-1.docker.io", "contentMaxAge": 1440, "metadataMaxAge": 1440},
  "negativeCache": {"enabled": true, "timeToLive": 1440},
  "httpClient": {"blocked": false, "autoBlock": true},
  "docker": {"v1Enabled": false, "forceBasicAuth": false, "httpPort": 8082},
  "dockerProxy": {"indexType": "HUB"}
}'
creer_depot docker-hosted hosted '{
  "name": "docker-hosted", "online": true,
  "storage": {"blobStoreName": "default", "strictContentTypeValidation": true, "writePolicy": "allow"},
  "docker": {"v1Enabled": false, "forceBasicAuth": true, "httpPort": 8083}
}'

# 6. Groupe Maven utilisé par .mvn/nexus-settings.xml
curl "${ADMIN[@]}" "$API/repositories" | grep -q '"name" *: *"maven-public"' \
  && echo "✅ maven-public présent" || { echo "❌ maven-public absent"; exit 1; }

echo "Maven : cd backend && NEXUS_URL=$HOTE ./mvnw -s .mvn/nexus-settings.xml verify"
echo "Images : REGISTRY=localhost:8082 docker compose build"
