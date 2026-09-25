# KFOKAM48 — Présence & Relecture

Application web de la formation KFOKAM48 : le formateur ouvre une session et projette un code de présence, les étudiants pointent avec ce code puis déposent le lien de leur exercice, un pair tiré au sort le note sur 20, et le formateur suit présences et moyennes dans un tableau.

**Auteur :** TCHANA Franck Hervé · matricule KF48-YAO-260

## Choix techniques

| Partie | Choix |
|---|---|
| Backend | Java 17, Spring Boot 3.5, Maven (wrapper `mvnw`), PostgreSQL 16, Flyway |
| Frontend | **React (Vite + TypeScript)** : trois écrans simples n'ont besoin ni du rendu serveur de Next.js ni de l'outillage d'Angular, c'est l'option la plus légère à construire, tester et démarrer depuis un clone vierge |
| Démarrage | `docker compose up --build` (section « Démarrer ») |

## Démarrer

Prérequis : Docker avec Docker Compose v2. Rien d'autre (ni Java, ni PostgreSQL, ni Maven).

```bash
git clone https://github.com/FranckHerve100/kfokam48-epreuve-KF48-YAO-260.git
cd kfokam48-epreuve-KF48-YAO-260
docker compose up --build
```

L'application est prête quand le conteneur `backend` est *healthy* (environ une minute au premier build). Arrêt : `Ctrl+C`, puis `docker compose down -v` pour effacer aussi la base.

| Adresse | Contenu |
|---|---|
| http://localhost:8080/swagger-ui.html | Swagger UI : définitions « Contrat imposé » et « Implémentation » |
| http://localhost:8080/api/… | API REST ([contrat](api/contrat.yaml)) |
| http://localhost:8080/actuator/health | Santé (application et base) ; aussi `/info`, `/metrics`, `/prometheus` |

**Données de démonstration** (chargées au démarrage) : promotions « KF48 Yaoundé » (id 1, 6 étudiants) et « KF48 Douala » (id 2, 2 étudiants) ; session au code `EXPR22` (expiré), session clôturée `CLTR23`, session `KF48YD` ouverte 15 minutes au premier démarrage ; 2 exercices, dont un relu (15/20) et un en attente de relecture.

**Vérifier l'installation :**

```bash
bash scripts/smoke.sh                                   # tests de fumée
npx --yes newman run postman/KFOKAM48.postman_collection.json -e postman/local.postman_environment.json
```

La collection Postman s'importe aussi dans Postman ([postman/README.md](postman/README.md)). Variables facultatives : copier `.env.example` en `.env` (port, identifiants de la base).

**Développement du backend** (Java 17) : `cd backend && ./mvnw verify` lance tous les tests sur H2, sans base installée.

**Développement du frontend** (Node 22+) : avec le backend démarré (`docker compose up`), `cd frontend && npm ci && npm run dev` ouvre http://localhost:5173 ; Vite relaie `/api` vers http://localhost:8080. `npm run build` produit `frontend/dist/`.

## Documentation

| Document | Contenu |
|---|---|
| [Cahier des charges](docs/CAHIER_DES_CHARGES.md) | Exigences EF1–EF11, ENF1–ENF8, règles RG1–RG18, hypothèses et contradictions tranchées |
| [Architecture](docs/ARCHITECTURE.md) | Conteneurs, couches, CI/CD, décisions ADR |
| [Diagrammes](docs/diagrammes/) | D1 cas d'utilisation, D2 modèle de données, D3 séquence « marquer sa présence », D4 états d'un exercice |
| [Contrat d'API](api/contrat.yaml) | 5 opérations imposées, inchangées, et opérations ajoutées |
| [Contribuer](CONTRIBUTING.md) | Branches, commits, Definition of Done |

## Structure

```
/api        contrat.yaml (OpenAPI 3)
/docs       CAHIER_DES_CHARGES.md · ARCHITECTURE.md · JOURNAL.md · diagrammes/
/postman    collection Postman / Newman
/scripts    smoke.sh (tests de fumée)
/backend    Spring Boot : API, migrations Flyway, tests
/frontend   React + Vite + TypeScript : src/api/client.ts (seul point d'appel HTTP), src/pages/ (3 écrans)
```

## Suivi

Backlog : [issues](https://github.com/FranckHerve100/kfokam48-epreuve-KF48-YAO-260/issues) · tableau Kanban : projet « KFOKAM48 – KF48-YAO-260 ».
