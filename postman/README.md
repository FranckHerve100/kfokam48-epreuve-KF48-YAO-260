# Tests d'API Postman / Newman

| Fichier | Contenu |
|---|---|
| `KFOKAM48.postman_collection.json` | Collection Postman v2.1, un dossier par ressource, des tests `pm.test` sur chaque requête (statut, `code`, champs obligatoires) |
| `local.postman_environment.json` | Environnement `baseUrl = http://localhost:8080` |

Dossiers, exécutés dans l'ordre : `00-Santé`, `01-Référentiel`, `02-Sessions`, `03-Présences`, `04-Exercices`, `05-Relectures`, `06-Tableau`, `99-Erreurs` (les dossiers s'ajoutent avec les tickets). Les requêtes enchaînent leurs identifiants par des variables de collection : la collection s'exécute sans intervention, sur une base neuve.

## Dans Postman

1. *Import* → sélectionner les deux fichiers de ce dossier.
2. Choisir l'environnement « KFOKAM48 local » en haut à droite.
3. Démarrer l'application (`docker compose up --build` à la racine), puis *Run collection*.

## En ligne de commande (Newman)

```bash
docker compose up -d --build --wait
npx --yes newman run postman/KFOKAM48.postman_collection.json -e postman/local.postman_environment.json
docker compose down -v
```

Rapport JUnit (utilisé par la CI) : ajouter `--reporters cli,junit --reporter-junit-export newman/junit.xml` (le dossier `newman/` est ignoré par Git).
