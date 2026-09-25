# Contribuer

## GitHub Flow

1. Partir de `main` à jour : `git switch main && git pull --ff-only`.
2. Une issue par ticket, avec ses critères d'acceptation. Aucun travail sans issue.
3. Une branche par issue : `<type>/<n°issue>-<slug>`, par exemple `feat/6-marquer-presence`, `chore/1-init-backend-flyway`, `fix/18-code-expire`.
4. Pour chaque critère : le test d'abord (rouge), puis le code (vert), puis le commit. Pousser après chaque commit.
5. Une PR par branche, avec le template rempli, qui cite l'issue :
   - `Closes #n` si la PR livre tout le ticket ;
   - `Refs #n` si elle n'en livre qu'une partie (par exemple le backend d'une story dont l'écran viendra plus tard).
6. Fusion par **merge commit** uniquement, après une CI verte. Pas de squash, pas de rebase, pas de `push --force`.
7. Pour mettre une branche à jour : `git merge origin/main`.

`main` n'accepte que des PR. Seuls les commits de jalon `[JALON] analyse`, `[JALON] v0.1` et `[JALON] v1.0` y sont posés directement.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/fr/), en français, au présent, 72 caractères au plus, une intention par commit :

```
<type>(<portée>): <ce que fait le commit> (RGx)
```

| Types | Portées |
|---|---|
| `feat`, `fix`, `test`, `refactor`, `chore`, `docs`, `build`, `ci` | `back`, `api`, `db`, `ci`, `infra`, `docker`, `sonar`, `nexus`, `docs` |

Exemples : `test(back): présence refusée 16 min après l'ouverture (RG1)` · `feat(back): renvoyer 410 CODE_EXPIRE sur code expiré (RG1)`.

## Règles non négociables

- Les 5 opérations imposées de `api/contrat.yaml` ne changent jamais : chemins, verbes, corps, codes de statut. Un cas nouveau réutilise un statut existant et se distingue par `code`.
- Une migration Flyway fusionnée ne se modifie jamais : on ajoute `V<n+1>__…sql` et on met à jour D2 dans la même PR.
- Aucun secret (`.env`, token, mot de passe) ni fichier généré (`target/`, `node_modules/`, `dist/`) dans Git.
- Aucun test supprimé, désactivé ou affaibli sans issue qui le justifie. Un bug corrigé commence par un test `NR_<n°issue>_…` qui le reproduit.
- Les tests portent le nom de la règle testée : `RG1_codeExpireApres15Minutes_renvoie410`.

## Definition of Done

Un ticket est terminé quand :

- ses critères d'acceptation sont couverts par des tests automatisés qui passent, nommés d'après les RG ;
- la suite complète passe **en local** : `./mvnw -B -ntp clean verify` (unitaires, intégration, conformité au contrat, ArchUnit, couverture JaCoCo ≥ 70 % sur les services) — la CI ne rejoue que les tests unitaires ;
- l'application démarre avec `docker compose up -d --build --wait`, puis `bash scripts/smoke.sh` et la collection Newman passent (porte locale, hors CI) ;
- chaque critère Given / When / Then de l'issue a été vérifié à la main et coché, avec la preuve en commentaire ;
- la CI est verte sur la PR (`prebuild`, `tests-unitaires`, `sonar` s'il est configuré), qui cite l'issue, les EF et les RG ;
- le contrat, les migrations, D2 et la documentation touchée sont à jour ;
- la PR est fusionnée par merge commit et `main` a été revérifié après la fusion ;
- le nombre de tests n'a pas diminué et la couverture n'a pas baissé.
