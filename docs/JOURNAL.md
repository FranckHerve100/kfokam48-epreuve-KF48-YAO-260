# Journal de bord — KF48-YAO-260

## Étape 1 — Analyse et conception
**Fait :** cahier des charges (10 sections, EF1–EF11, RG1–RG18, H1–H11), diagrammes D1 à D4 en Mermaid,
contrat complété (8 opérations ajoutées, imposées inchangées), architecture, 16 issues + projet Kanban.
**Bloqué :** sur les codes de statut des cas ajoutés aux opérations imposées : le cahier des charges prévoyait
403 `HORS_PROMOTION` et 429 `CODE_BLOQUE`, alors que le contrat fige les statuts de ces opérations. Tranché :
statut existant (400) et nouvelle valeur de `code` (H4, H6) ; D3 et le cahier des charges alignés avant le jalon.
**IA :** création des 16 issues avec gh depuis le backlog ; complétion du contrat et alignement de D3.
Vérifié : relecture des issues sur GitHub (titres, labels, milestones, champs du projet) ; comparaison
automatique des 5 opérations imposées avec le contrat fourni ; lint Redocly sans avertissement.

## Étape 2 — Première version (v0.1)
**Fait :** backend Spring Boot 3.5 (Flyway V1 = D2, erreurs `{ code, message }` partout, y compris hors MVC),
EF1 à EF6 côté API (5 opérations imposées conformes au contrat), tirage du relecteur par événements de domaine,
trois écrans React (formateur, étudiant, relecteur), `docker compose up --build` qui démarre base, API et écrans
avec des données de démonstration. Outillage : CI (prebuild → tests unitaires → Sonar → build sur `main`),
SonarQube et Nexus optionnels, CodeQL / Gitleaks / Dependabot. 131 tests unitaires, 42 d'intégration.
**Bloqué :** GitHub Actions ne démarre aucun job (compte bloqué pour facturation) : contourné en rejouant chaque
étape du pipeline en local, toujours ouvert (#22). ~15 min : une PR Dependabot (springdoc 3.x, pour Spring Boot 4)
fusionnée par erreur a cassé `main` — issue #33, test `NR_33`, revert par PR, Dependabot limité aux versions
mineures. ~10 min : échecs intermittents de `verify` causés par l'éditeur qui compilait dans `target/` en même
temps que Maven ; réglé en lançant toujours `clean verify`. Quelques minutes sur la licence de Nexus Community
Edition, qui refuse tout téléchargement tant qu'elle n'est pas acceptée.
**IA :** backend, tests, pipeline, conteneurs et écrans générés ticket par ticket, dans des branches et des PR
séparées. Vérifié : test rouge constaté avant chaque correctif ; `./mvnw clean verify` avant chaque commit ;
réponses validées contre `api/contrat.yaml` (validateur Atlassian) ; fumée et collection Newman contre les
conteneurs ; démarrage depuis un clone vierge ; parcours des trois écrans dans Chrome à 1280 et 375 px.
Erreurs trouvées ainsi : page d'erreur HTML de Tomcat sur une URL illisible, `git.properties` vide dans l'image,
code de session qui débordait de sa carte, page trop large sur téléphone.

## Étape 3 — Enveloppe
**Fait :** bug « deux étudiants pointent en même temps, un seul apparaît » : issue #50 ouverte avant tout code,
test `NR_50` commité rouge (deux transactions réelles entrelacées), puis correctif — le tirage relancé par une
présence s'exécute après la validation de la présence, dans sa propre transaction (PR #51). Changement de besoin
« deux relecteurs, note = moyenne, provisoire si une seule » : issues #52 et #53, analyse remise à jour d'abord
(cahier des charges v2, D2, D4), contrat (`moyenneProvisoire`), migration `V2__deux_relecteurs.sql` ajoutée sans
toucher V1, tirage de deux relecteurs, statut `PARTIELLEMENT_RELU`, tableau et écran formateur ; branche et PR
distinctes du correctif.
**Bloqué :** la cause du bug n'était pas visible dans le code des présences lui-même : elle venait du tirage relancé
dans la même transaction. Il a fallu entrelacer deux transactions à la main dans le test pour la reproduire à coup sûr.
**IA :** diagnostic du bug, test de concurrence, correctif, migration et adaptation du tirage et du tableau.
Vérifié : test `NR_50` rouge avant le correctif puis vert ; 5 pointages parallèles sur PostgreSQL → 5 × 201 et un
seul relecteur ; migration V2 appliquée sur une base v0.1 remplie (mêmes volumes de données avant et après) ;
parcours complet à deux relecteurs sur PostgreSQL (note provisoire puis moyenne).
**Ce que j'ai sorti du périmètre pour absorber le changement, et pourquoi :** EF11 (Could) puis EF10 (Should). Le
changement est un Must qui arrive tard ; EF10 aurait dû être refaite (note provisoire, deux commentaires) alors que
la note reste consultable par le formateur, et EF11 est la seule Could. EF7, EF8 et EF9 restent prévues pour la v1.0.
Écrit aussi au §10 du cahier des charges et sur les issues #15 et #16.
