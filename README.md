# KFOKAM48 — Présence & Relecture

Application web de la formation KFOKAM48 : le formateur ouvre une session et projette un code de présence, les étudiants pointent avec ce code puis déposent le lien de leur exercice, un pair tiré au sort le note sur 20, et le formateur suit présences et moyennes dans un tableau.

**Auteur :** TCHANA Franck Hervé · matricule KF48-YAO-260

## Choix techniques

| Partie | Choix |
|---|---|
| Backend | Java 17, Spring Boot 3.5, Maven (wrapper `mvnw`), PostgreSQL 16, Flyway |
| Frontend | **React (Vite + TypeScript)** : trois écrans simples n'ont besoin ni du rendu serveur de Next.js ni de l'outillage d'Angular, c'est l'option la plus légère à construire, tester et démarrer depuis un clone vierge |
| Démarrage | `docker compose up` (section « Démarrer », à venir avec le ticket #11) |

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
/backend    Spring Boot (à venir)
/frontend   React (à venir)
```

## Suivi

Backlog : [issues](https://github.com/FranckHerve100/kfokam48-epreuve-KF48-YAO-260/issues) · tableau Kanban : projet « KFOKAM48 – KF48-YAO-260 ».
