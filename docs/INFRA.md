# Outillage optionnel — SonarQube et Nexus

Ces outils servent au développement : mesure de la qualité, cache des dépendances, registre d'images. **Ils ne sont jamais nécessaires** pour construire, tester ou démarrer l'application : `./mvnw verify` et `docker compose up` fonctionnent sans eux (ARCHITECTURE.md, ADR-8).

Tout se trouve dans `infra/docker-compose.infra.yml`, séparé du `docker-compose.yml` de l'application.

## Prérequis

| Besoin | Valeur |
|---|---|
| RAM libre | ~3 Go pour SonarQube et sa base |
| Linux : `vm.max_map_count` | ≥ 524288 pour SonarQube (Elasticsearch embarqué) : `sudo sysctl -w vm.max_map_count=524288` ; à rendre permanent dans `/etc/sysctl.d/99-sonarqube.conf` |
| Fichier `.env` | Copie de `.env.example` à la racine, **jamais commitée** |

## SonarQube

### Démarrer

```bash
cp -n .env.example .env        # puis renseigner SONAR_ADMIN_PASSWORD
docker compose -f infra/docker-compose.infra.yml --env-file .env up -d sonarqube
```

SonarQube répond sur http://localhost:9000 après une à deux minutes. L'image n'embarquant ni `curl` ni `wget`, le conteneur n'a pas de healthcheck : c'est `init.sh` qui attend `GET /api/system/status` = `UP`.

### Initialiser (idempotent)

```bash
bash infra/sonar/init.sh
```

Le script lit `.env` et :

1. remplace le mot de passe par défaut `admin` par `SONAR_ADMIN_PASSWORD` (12 caractères au moins, avec majuscule, minuscule, chiffre et caractère spécial) ;
2. crée le projet `kfokam48-presence` s'il n'existe pas ;
3. génère un token d'analyse **seulement si** `SONAR_TOKEN` est absent ou invalide, et l'affiche une seule fois : le copier dans `.env`.

Relancé, il ne modifie rien.

### Analyser

```bash
set -a; source .env; set +a
cd backend
./mvnw verify sonar:sonar -Dsonar.host.url="$SONAR_HOST_URL" -Dsonar.token="$SONAR_TOKEN" -Dsonar.qualitygate.wait=true
```

- La couverture vient du rapport JaCoCo `target/site/jacoco/jacoco.xml` (tests unitaires **et** d'intégration).
- Exclus de la couverture : `dto/**`, `domain/**`, `config/**`, `*Application*` (records, entités et configuration sans logique).
- `-Dsonar.qualitygate.wait=true` fait échouer la commande si le Quality Gate est rouge.
- Tableau de bord : http://localhost:9000/dashboard?id=kfokam48-presence

Première analyse (ticket #24) : Quality Gate **PASSED**, couverture 87,7 % (lignes 94,9 %), 0 bug, 0 vulnérabilité, 0 duplication ; les 3 *code smells* relevés ont été corrigés dans la même PR.

### En CI

Le job `sonar` de `ci.yml` (après `tests-unitaires`, sans relancer les tests : il analyse les classes et le rapport JaCoCo du job précédent, donc la couverture des tests unitaires) s'exécute seulement si la **variable** de dépôt `SONAR_HOST_URL` et le **secret** `SONAR_TOKEN` sont définis (SonarQube Cloud, gratuit pour un dépôt public, ou un serveur joignable depuis GitHub). Sinon il est ignoré, jamais en échec, et il ne fait pas partie des checks requis de `main`.

### Arrêter

```bash
docker compose -f infra/docker-compose.infra.yml down        # garde les données
docker compose -f infra/docker-compose.infra.yml down -v     # efface tout
```

## Nexus

Nexus Repository Community Edition sert à trois choses : cache de Maven Central (`maven-public`), cache de Docker Hub (`docker-proxy`, port 8082) et registre privé des images du projet (`docker-hosted`, port 8083).

### Démarrer

```bash
docker compose -f infra/docker-compose.infra.yml --env-file .env up -d --wait nexus
```

Interface : http://localhost:8081 (prête après une à deux minutes, healthcheck sur `/service/rest/v1/status/writable`).

### Initialiser (idempotent)

Dans `.env` : `NEXUS_ADMIN_PASSWORD` et `NEXUS_EULA_ACCEPTEE=true`. La Community Edition refuse tout téléchargement (403) tant que sa [licence](https://links.sonatype.com/products/nxrm/ce-eula) n'est pas acceptée : cette variable matérialise la décision de l'accepter, le script ne l'accepte jamais sans elle.

```bash
bash infra/nexus/init.sh
```

Le script, via l'API REST :

1. remplace le mot de passe initial (`/nexus-data/admin.password`) par `NEXUS_ADMIN_PASSWORD` ;
2. accepte la licence si `NEXUS_EULA_ACCEPTEE=true` ;
3. active l'accès anonyme en lecture et le *Docker Bearer Token Realm* (pull anonyme) ;
4. crée `docker-proxy` (Docker Hub, port 8082) et `docker-hosted` (port 8083, authentification exigée) ;
5. vérifie que le groupe Maven `maven-public` existe.

### Dépendances Maven via Nexus

```bash
set -a; source .env; set +a
cd backend
./mvnw -s .mvn/nexus-settings.xml verify
```

`.mvn/nexus-settings.xml` redirige tous les dépôts vers `${NEXUS_URL}/repository/maven-public/`. Sans `-s`, Maven utilise Maven Central directement.

Vérification (ticket #26) : avec un dépôt local vide (`-Dmaven.repo.local=…`), `./mvnw -s .mvn/nexus-settings.xml -U clean verify` est vert et 544 composants apparaissent dans `maven-central` (`GET /service/rest/v1/components?repository=maven-central`).

### Images via Nexus

Les images de base (`maven`, `eclipse-temurin`) passent par le proxy si `REGISTRY` vaut `localhost:8082` ; le build Maven de l'image passe par Nexus si `MAVEN_MIRROR_URL` est défini (`host.docker.internal` est déclaré dans `docker-compose.yml`) :

```bash
REGISTRY=localhost:8082 \
MAVEN_MIRROR_URL=http://host.docker.internal:8081/repository/maven-public/ \
docker compose build --pull backend
```

Docker accepte `localhost:8082` et `localhost:8083` en HTTP sans configuration (registres locaux).

### Publier l'image du backend

```bash
set -a; source .env; set +a
printf '%s' "$NEXUS_ADMIN_PASSWORD" | docker login localhost:8083 -u admin --password-stdin
VERSION=0.1.0-SNAPSHOT
docker tag kfokam48/presence-backend:$VERSION localhost:8083/kfokam48/presence-backend:$VERSION
docker tag kfokam48/presence-backend:$VERSION localhost:8083/kfokam48/presence-backend:latest
docker push localhost:8083/kfokam48/presence-backend:$VERSION
docker push localhost:8083/kfokam48/presence-backend:latest
```

Récupération sur un autre poste du réseau : `docker pull localhost:8083/kfokam48/presence-backend:0.1.0-SNAPSHOT` (lecture anonyme). La CI GitHub n'utilise pas Nexus, qui n'est pas joignable depuis GitHub : elle garde le cache de `actions/setup-java`.

### Arrêter

```bash
docker compose -f infra/docker-compose.infra.yml stop nexus
```

Nexus arrêté, tout reste fonctionnel : `./mvnw verify` et `docker compose up --build` utilisent Maven Central et Docker Hub.
