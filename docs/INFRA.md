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

Le job `sonar` de `ci.yml` s'exécute seulement si la **variable** de dépôt `SONAR_HOST_URL` et le **secret** `SONAR_TOKEN` sont définis (SonarQube Cloud, gratuit pour un dépôt public, ou un serveur joignable depuis GitHub). Sinon il est ignoré, jamais en échec, et il ne fait pas partie des checks requis de `main`.

### Arrêter

```bash
docker compose -f infra/docker-compose.infra.yml down        # garde les données
docker compose -f infra/docker-compose.infra.yml down -v     # efface tout
```
