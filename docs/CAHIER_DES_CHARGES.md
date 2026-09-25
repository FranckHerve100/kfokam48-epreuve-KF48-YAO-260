# Cahier des charges — KFOKAM48 Présence & Relecture

**Auteur :** TCHANA Franck Hervé · KF48-YAO-260
**Version :** 1 · **Date :** 25/09/2026
**Frontend choisi :** React (Vite + TypeScript), parce que trois écrans simples n'ont besoin ni du rendu serveur de Next.js ni de l'outillage d'Angular : c'est l'option la plus légère à construire, tester et démarrer depuis un clone vierge.

---

## 1. Contexte et objectif

La formation KFOKAM48 fait l'appel et la relecture des exercices à la main : les présences sont notées sur papier, les liens d'exercices circulent par messagerie et personne ne sait qui a relu quoi. Le formateur n'a donc aucune vue fiable de l'assiduité ni du niveau de chaque étudiant.

L'application remplace ce fonctionnement par un circuit unique :

1. Le formateur ouvre une session et projette un code valable 15 minutes.
2. Les étudiants pointent avec ce code depuis leur téléphone, puis déposent le lien de leur exercice.
3. Le système confie chaque exercice à un pair présent, tiré au sort, qui le note sur 20 et le commente.
4. Le formateur suit dans un tableau, par étudiant : présences, exercices déposés, moyenne des notes reçues et relectures encore dues.

L'objectif est de rendre l'assiduité et la relecture entre pairs traçables, équitables et consultables en un seul écran.

## 2. Acteurs et rôles

| Acteur | Ce qu'il peut faire | Ce qu'il ne peut pas faire |
|---|---|---|
| Formateur | Ouvrir une session pour une promotion et obtenir son code ; ajouter une présence à la main ; clôturer une session ; consulter le tableau d'une promotion | Noter un exercice ; voir qui a relu quel exercice depuis l'écran étudiant |
| Étudiant | Se choisir dans la liste de sa promotion (sans mot de passe) ; marquer sa présence avec le code ; déposer puis remplacer le lien de son exercice ; voir la note et le commentaire reçus | Marquer sa présence après expiration du code ; déposer deux exercices pour la même session ; connaître le nom de son relecteur |
| Relecteur | Noter de 0 à 20 et commenter l'exercice qui lui est assigné | Relire son propre exercice ; modifier une note déjà envoyée ; choisir l'exercice qu'il relit |
| Système | Générer et expirer le code ; bloquer un étudiant après 5 codes erronés ; tirer le relecteur au sort ; calculer la moyenne | — |

**Décision : le relecteur n'est pas un acteur distinct, c'est un étudiant dans un certain état.** Un étudiant devient relecteur d'un exercice précis au moment où le système le lui assigne. Il n'y a donc pas de table `relecteur` : la table `relecture` porte une clé étrangère `relecteur_id` vers `etudiant`, à côté de `exercice_id`. Un même étudiant peut être auteur d'un exercice et relecteur d'un autre dans la même session.

## 3. Périmètre

**Inclus dans cette version :**
- Les 5 opérations imposées du contrat d'API : ouverture de session, présence, dépôt d'exercice, relecture, tableau.
- Les opérations ajoutées au contrat pour couvrir les réponses du client : ajout manuel de présence, clôture de session, remplacement du lien, liste des promotions et des étudiants, relectures et exercices d'un étudiant.
- Les trois écrans : formateur, étudiant, relecteur.
- Le blocage temporaire après 5 codes erronés.
- Des données de démonstration chargées au démarrage.
- Le démarrage complet par `docker compose up`.

**Explicitement exclu :**
- Authentification et mots de passe (Q1) : l'étudiant choisit son nom dans une liste.
- Gestion des promotions et des étudiants (création, import CSV) : ils sont fournis par les données de démonstration.
- Plusieurs relecteurs par exercice (Q6), ou une réassignation manuelle par le formateur.
- Notifications (e-mail, SMS, push).
- Soin graphique et CSS avancé (non évalué).
- Hébergement public en ligne : l'application se lance en local.
- Export du tableau (PDF, Excel).

## 4. Exigences fonctionnelles

| Réf | Exigence | Critère d'acceptation | Priorité |
|---|---|---|---|
| EF1 | Le formateur ouvre une session et obtient un code de présence | Quand j'ouvre une session pour une promotion existante à 10h00, alors je reçois un code unique parmi les sessions ouvertes et une expiration à 10h15 (201) | Must |
| EF2 | L'étudiant marque sa présence avec le code | Quand je choisis mon nom et saisis un code valide non expiré, alors ma présence est créée avec la source `ETUDIANT` et apparaît dans le tableau du formateur ; code expiré → 410, déjà présent → 409, code inconnu → 400 | Must |
| EF3 | L'étudiant dépose le lien de son exercice | Quand je dépose un lien https valide sur une session non clôturée, alors je reçois 201 avec l'identifiant et le statut de l'exercice ; lien invalide → 400, second dépôt → 409 | Must |
| EF4 | Un relecteur est assigné au hasard à chaque exercice déposé | Quand un exercice est déposé et qu'au moins un autre étudiant est présent, alors un seul relecteur, différent de l'auteur, lui est assigné et le statut passe à `EN_ATTENTE_RELECTURE` ; sinon le statut reste `DEPOSE` jusqu'à la prochaine présence | Must |
| EF5 | Le relecteur rend une note et un commentaire | Quand j'envoie une note entière de 0 à 20 sur une relecture qui m'est assignée, alors je reçois 200 et l'exercice passe à `RELU` ; note hors bornes ou décimale → 400, mon propre exercice → 403, second envoi → 409 | Must |
| EF6 | Le formateur voit le tableau de sa promotion | Quand j'ouvre le tableau d'une promotion, alors je vois pour chaque étudiant son nombre de présences, ses exercices déposés, la moyenne des notes reçues calculée par l'API et ses relectures en attente ; promotion inconnue → 404 | Must |
| EF7 | Le formateur ajoute une présence à la main | Quand j'ajoute la présence d'un étudiant sur une session non clôturée, même après expiration du code, alors elle est créée avec la source `FORMATEUR` et le tableau l'indique | Should |
| EF8 | L'étudiant est bloqué après 5 codes erronés | Quand un étudiant saisit un 5e code erroné consécutif, alors toute nouvelle saisie dans les 2 minutes renvoie 400 `CODE_BLOQUE`, même avec le bon code | Should |
| EF9 | Le formateur clôture une session | Quand je clôture une session, alors les dépôts et les ajouts de présence sur cette session sont refusés (409), et les relectures en attente restent visibles au tableau | Should |
| EF10 | L'étudiant voit sa note et le commentaire reçus | Quand mon exercice a été relu, alors je vois la note et le commentaire, et la réponse de l'API ne contient ni l'identifiant ni le nom du relecteur | Should |
| EF11 | L'étudiant remplace le lien de son exercice | Quand je remplace le lien d'un exercice pas encore relu sur une session non clôturée, alors je reçois 200 et le nouveau lien est enregistré ; exercice déjà relu → 409 | Could |

## 5. Exigences non fonctionnelles

| Réf | Exigence | Comment on la vérifie |
|---|---|---|
| ENF1 | L'interface de marquage de présence est utilisable sur un téléphone | Écran étudiant ouvert dans les outils de développement du navigateur en largeur 375 px : aucun défilement horizontal, champ du code et bouton visibles sans zoom |
| ENF2 | Le tableau du formateur répond en moins de 2 s pour une promotion de 60 étudiants | Test d'intégration qui charge 60 étudiants, 10 sessions et leurs exercices, puis vérifie que `GET /api/tableau` répond en moins de 2 s ; le calcul se fait en une requête agrégée, sans requête par étudiant |
| ENF3 | Volumétrie cible : 5 promotions de 60 étudiants et 3 sessions par semaine | Les données de test de ENF2 couvrent une promotion complète ; les index sur `session.code`, `presence(session_id, etudiant_id)` et `exercice(session_id, etudiant_id)` sont présents dans la migration |
| ENF4 | Toute erreur renvoie `{ code, message }`, sans stack trace | Tests MockMvc sur chaque code d'erreur du contrat, sur un JSON malformé et sur une route inconnue |
| ENF5 | L'application démarre chez un tiers avec la seule aide du README | `docker compose up` (ou 3 commandes au plus) exécuté depuis un clone vierge dans un autre dossier, avant le jalon v1.0 |
| ENF6 | Les tests tournent sur un poste vierge | `./mvnw test` passe sans base PostgreSQL installée (profil `test` sur H2), en local et dans la CI |
| ENF7 | Les horaires sont exacts quel que soit le fuseau du serveur | Dates stockées en UTC (`Instant`) ; test unitaire de RG1 avec une `Clock` fixe |
| ENF8 | Aucun secret dans le dépôt | `.env` ignoré, `.env.example` commité ; secret scanning et Gitleaks actifs sur le dépôt |

## 6. Règles de gestion

| Réf | Règle | Source |
|---|---|---|
| RG1 | Un code de présence expire 15 minutes après l'ouverture de la session ; au-delà, la saisie renvoie 410 `CODE_EXPIRE` | Q2, Q3 |
| RG2 | Un étudiant n'a qu'une présence par session ; un second pointage renvoie 409 `DEJA_PRESENT` | Contrat |
| RG3 | Un code qui ne correspond à aucune session ouverte renvoie 400 `CODE_INCONNU` | Contrat |
| RG4 | Après 5 codes erronés consécutifs, l'étudiant est bloqué 2 minutes (400 `CODE_BLOQUE`) ; un code correct remet le compteur à zéro | Q4 |
| RG5 | Le formateur peut ajouter une présence jusqu'à la clôture, même code expiré ; elle porte la source `FORMATEUR` | Q14, Q3 |
| RG6 | Un étudiant ne dépose qu'un exercice par session ; un second dépôt renvoie 409 `EXERCICE_DEJA_DEPOSE` | Contrat |
| RG7 | Le dépôt d'un exercice est possible jusqu'à la clôture de la session, même après expiration du code | Q12 |
| RG8 | Le lien d'un exercice peut être remplacé tant qu'aucune relecture n'a été rendue et que la session n'est pas clôturée | Q13 |
| RG9 | Un exercice a un seul relecteur | Q6 |
| RG10 | Le relecteur est tiré au hasard parmi les étudiants présents à la session, jamais l'auteur ; le moins chargé en relectures est choisi en premier, puis au hasard entre ex æquo ; relire son propre exercice renvoie 403 `AUTO_RELECTURE` | Q5, Q7 |
| RG11 | Une note est un entier compris entre 0 et 20 ; sinon 400 `NOTE_INVALIDE` | Q9 |
| RG12 | Une note est définitive dès son envoi ; un second envoi renvoie 409 `RELECTURE_DEJA_RENDUE` | Q15, contrat |
| RG13 | L'étudiant relu voit la note et le commentaire, jamais l'identité du relecteur | Q8 |
| RG14 | Un exercice non relu reste « en attente », sans limite de temps, et reste visible au tableau | Q11 |
| RG15 | La moyenne d'un étudiant est calculée par l'API sur les notes reçues, arrondie à 2 décimales ; elle vaut `null` s'il n'a reçu aucune note | Q16, F3 |
| RG16 | Le lien d'un exercice est une URL `http` ou `https` valide ; sinon 400 `LIEN_INVALIDE` | Contrat |
| RG17 | Toute erreur suit le format `{ code, message }` ; un champ obligatoire absent renvoie 400 `CHAMP_MANQUANT` | Contrat |
| RG18 | Seul un étudiant de la promotion de la session peut y marquer sa présence ou y déposer ; sinon 400 `HORS_PROMOTION` | Hypothèse H6 |

## 7. Zones d'ombre, hypothèses et contradictions

**Points que la demande ne tranche pas :**

| Point | Réponse client (Qx) ou hypothèse | Décision retenue | Conséquence |
|---|---|---|---|
| H1 — Aucun relecteur éligible (le trou) | Q7 choisit parmi les présents, Q12 autorise un dépôt tardif ; personne n'a prévu le cas où l'auteur est seul présent, ou n'était pas présent du tout | Tirage au dépôt parmi les présents sauf l'auteur, en commençant par le moins chargé ; si personne n'est éligible, l'exercice reste `DEPOSE` et le tirage est relancé à chaque nouvelle présence sur la session | Statut `DEPOSE` ajouté avant `EN_ATTENTE_RELECTURE` (diagramme D4) ; `relecture.relecteur_id` est rempli au tirage |
| H2 — « Fin de session » et « clôture » | Q3 parle de fin, Q10 et Q12 de clôture par le formateur | Deux notions distinctes : l'expiration du code (ouverture + 15 min) arrête le pointage étudiant ; la clôture explicite par le formateur arrête les dépôts et les ajouts de présence, et aussi le pointage étudiant si elle intervient avant l'expiration (410 `CODE_EXPIRE`) | Colonne `session.cloture_at` nullable ; nouvelle opération `POST /api/sessions/{id}/cloture` |
| H3 — « Personne n'a commencé à relire » | Q13 ; la relecture est un envoi unique, il n'existe pas d'état « en cours » | « Commencé » = relecture rendue. Remplacement du lien possible tant que la note n'est pas envoyée | Nouvelle opération `PUT /api/exercices/{id}` ; 409 `EXERCICE_EN_RELECTURE` |
| H4 — Réponse au blocage | Q4 demande un blocage de 2 min ; le contrat ne prévoit aucun code pour ce cas et impose de garder les codes de statut de `POST /api/presences` (201, 400, 409, 410) | Compteur d'erreurs par étudiant ; réponse 400 avec `code = CODE_BLOQUE`. 429 aurait été plus précis, mais il aurait ajouté un code de statut à une opération imposée | Colonnes `erreurs_code` et `bloque_jusqu_a` sur `etudiant` (D2) ; message dédié sur l'écran étudiant |
| H5 — Identifier la session sans `sessionId` | Le contrat de `POST /api/presences` n'envoie que `{ code, etudiantId }` | Le code est unique parmi les sessions non expirées : il identifie la session | Contrainte d'unicité applicative à la génération ; index sur `session.code` |
| H6 — Étudiant d'une autre promotion | Non abordé | Refusé : 400 `HORS_PROMOTION` (RG18), code de statut déjà prévu par le contrat | Nouvelle valeur de `code`, aucun nouveau code de statut |
| H7 — Identité sans mot de passe | Q1 : l'étudiant choisit son nom dans une liste | Accepté tel quel : un étudiant peut techniquement se faire passer pour un autre ; c'est un choix du client, noté comme risque | Aucune table d'utilisateurs ; `GET /api/promotions/{id}/etudiants` alimente la liste |
| H8 — Dépôt sans présence | Q12 autorise un dépôt le soir, donc potentiellement sans avoir pointé | Autorisé : le dépôt ne dépend pas de la présence. En revanche, seuls les présents peuvent relire | L'auteur absent reçoit quand même un relecteur parmi les présents |
| H9 — Relecture après clôture | Q11 laisse l'exercice « en attente » sans échéance | Autorisée : la clôture fige les dépôts, pas les relectures | Le tableau continue de compter la relecture en attente |
| H10 — Moyenne sans note | Q16 demande une moyenne | `null` si aucune note, affichée « — » ; arrondie à 2 décimales | Aucun calcul côté frontend (F3) |
| H11 — Qui envoie la relecture ? | Le contrat prévoit 403 « relecture de son propre exercice » mais le corps imposé `{ note, commentaire }` ne dit pas qui envoie | En-tête **optionnel** `X-Etudiant-Id` sur `POST /api/relectures/{id}` : le corps et les codes de statut imposés restent intacts. S'il est présent : 403 `AUTO_RELECTURE` si c'est l'auteur, 403 `RELECTEUR_NON_ASSIGNE` si ce n'est pas le relecteur tiré. Le frontend l'envoie toujours | Un appel strictement conforme au contrat d'origine reste accepté |

**Contradictions relevées :**

| Réponses en conflit | Ce que j'ai choisi | Pourquoi |
|---|---|---|
| Q10 (« le relecteur peut corriger sa note tant que la session n'est pas clôturée ») contre Q15 (« une fois validée, c'est fini, il ne peut plus revenir ») | **Q15** : la note est définitive dès l'envoi (RG12) | Le contrat imposé prévoit déjà « 409 relecture déjà rendue » sur `POST /api/relectures/{id}` : suivre Q10 aurait contredit le contrat, qui est non négociable (B2). Q15 est aussi la règle la plus équitable, comme le dit le client lui-même |
| Q3 (« on ne peut pas marquer sa présence après la fin de la session ») contre Q14 (« le formateur peut ajouter une présence à la main ») | Q3 s'applique à l'étudiant ; le formateur peut ajouter une présence jusqu'à la clôture, marquée `FORMATEUR` (RG5) | Q14 décrit un cas réel (téléphone en panne) qui arrive précisément après l'expiration du code ; la source `FORMATEUR` garde la trace demandée |

## 8. Contraintes techniques

**Imposées par le sujet :**

| Réf | Contrainte | Comment je la respecte |
|---|---|---|
| B1 | Java 17+, Maven, wrapper `mvnw` commité | Java 17, Spring Boot 3, `mvnw` et `.mvn/wrapper/` versionnés (exception dans le `.gitignore`) |
| B2 | Contrat `api/contrat.yaml` respecté à la lettre | Contrat complété et figé avant le premier commit de code ; un test d'intégration par ligne du contrat ; lint Redocly en CI |
| B3 | Séparation contrôleur / service / repository, DTO | Paquets `controller`, `service`, `repository`, `domain`, `dto` ; test ArchUnit qui interdit à un contrôleur d'utiliser un repository ou une entité |
| B4 | Validation des entrées et erreurs centralisées | Bean Validation sur les DTO ; un seul `@RestControllerAdvice` ; aucune stack trace renvoyée |
| B5 | Schéma versionné par Flyway | `V1__schema.sql` conforme au diagramme D2 ; `ddl-auto=validate` ; une migration ne se modifie jamais, on en ajoute une nouvelle |
| B6 | Un test unitaire sur une règle métier, un test d'intégration sur un endpoint, sur un poste vierge | Test unitaire de RG1 avec une `Clock` fixe ; test MockMvc de `POST /api/presences` (201, 409, 410) ; profil `test` sur H2 |
| F1 | Framework déclaré et justifié, build qui passe | React, justifié dans le README ; `npm run build` en CI |
| F2 | Trois écrans | Formateur (ouvrir une session, tableau), étudiant (présence, dépôt, note reçue), relecteur (relecture) |
| F3 | Couche API dédiée, états gérés, aucune règle dupliquée | `src/api/client.ts` est le seul point d'appel ; états chargement/erreur par écran ; moyenne lue dans l'API |

**Que je m'impose :**
- Base de données : PostgreSQL 16 dans `docker compose` ; H2 en mode PostgreSQL pour les tests.
- Données de démonstration : migration répétable `R__demo_data.sql` (1 promotion, 6 étudiants, 1 session, présences, exercices, relectures).
- Temps : dates en UTC (`Instant`), `Clock` injectée pour tester RG1 et RG4 ; `Random` injecté pour tester RG10.
- Tests nommés d'après la règle testée (`RG1_codeExpireApres15Minutes_renvoie410`).
- CI GitHub Actions sur chaque PR : build, lint, tests, couverture JaCoCo ≥ 70 % sur les services, CodeQL et Gitleaks.
- Git : GitHub Flow, une branche et une PR par issue, merge commit, Conventional Commits citant les RG.

**Extensions du contrat d'API** (ajoutées avant le premier commit de code). Règle suivie : le contrat fourni impose « mêmes chemins, mêmes verbes, mêmes codes de statut, même format d'erreur » pour ses cinq opérations ; elles sont donc laissées intactes et toutes les nouveautés passent par des opérations ajoutées ou par de nouvelles valeurs de `code`.

| Opération | Succès | Erreurs |
|---|---|---|
| `GET /api/promotions` | 200 `[ { id, nom } ]` | — |
| `GET /api/promotions/{id}/etudiants` | 200 `[ { id, nom } ]` | 404 `PROMOTION_INCONNUE` |
| `GET /api/promotions/{id}/sessions` | 200 `[ { id, titre, code, ouvertureAt, expirationAt, clotureAt } ]` | 404 `PROMOTION_INCONNUE` |
| `POST /api/sessions/{id}/presences` `{ etudiantId }` | 201 `{ id, sessionId, etudiantId, source: "FORMATEUR" }` | 409 `DEJA_PRESENT`, 409 `SESSION_CLOTUREE` |
| `POST /api/sessions/{id}/cloture` | 200 `{ id, clotureAt }` | 409 `SESSION_CLOTUREE` |
| `PUT /api/exercices/{id}` `{ lien }` | 200 `{ id, statut }` | 400 `LIEN_INVALIDE`, 409 `EXERCICE_EN_RELECTURE`, 409 `SESSION_CLOTUREE` |
| `GET /api/etudiants/{id}/relectures` | 200 `[ { id, exerciceId, lien, statut } ]` | 404 `ETUDIANT_INCONNU` |
| `GET /api/etudiants/{id}/exercices` | 200 `[ { id, sessionId, lien, statut, note, commentaire } ]` | 404 `ETUDIANT_INCONNU` |
| Opérations imposées | Inchangées | Aucun code de statut ajouté. Les cas non prévus réutilisent les codes existants et se distinguent par `code` : `POST /api/sessions` 400 `PROMOTION_INCONNUE` · `POST /api/presences` 400 `CODE_BLOQUE`, `HORS_PROMOTION`, `ETUDIANT_INCONNU` · `POST /api/exercices` 400 `SESSION_INCONNUE`, `HORS_PROMOTION` et 409 `SESSION_CLOTUREE` · `POST /api/relectures/{id}` 400 `RELECTURE_INCONNUE`, 403 `RELECTEUR_NON_ASSIGNE`, en-tête optionnel `X-Etudiant-Id` · `GET /api/tableau` 404 aussi si `promotionId` est absent |

## 9. Livrables

- Dépôt public `kfokam48-epreuve-KF48-YAO-260`, historique lisible, trois commits `[JALON]` dans l'ordre.
- `docs/CAHIER_DES_CHARGES.md` : ce document, tenu à jour après l'étape 3.
- `docs/diagrammes/` en Mermaid : D1 cas d'utilisation, D2 modèle de données, D3 séquence « marquer sa présence », D4 états-transitions d'un exercice (bonus).
- `api/contrat.yaml` complété et figé avant le premier commit de code.
- Backlog en issues GitHub (16 issues au départ), rattachées aux milestones et au projet Kanban.
- `backend/` : API Spring Boot, migrations Flyway, tests.
- `frontend/` : application React, trois écrans.
- `docker-compose.yml`, `.env.example`, données de démonstration.
- `README.md` (installation testée depuis un clone vierge), `CONTRIBUTING.md`, `CHANGELOG.md`.
- `docs/JOURNAL.md` : une entrée par étape.
- Second dépôt public `kfokam48-gitlab-KF48-YAO-260` (épreuve Git).
- `SOUMISSION.md` déposé sur la plateforme avant 18h00, avec les hash complets des deux commits finaux.

## 10. Démarche prévue

| Étape | Ce que je vise | Se termine par |
|---|---|---|
| 1. Analyse (~35 % du temps) | `.gitignore` puis templates GitHub ; ce cahier des charges ; diagrammes D1 à D4 ; contrat complété et figé ; 16 issues ; journal | `[JALON] analyse` |
| 2. Version 0.1 | Socle (backend, erreurs, frontend, CI, docker compose), puis EF1 à EF6 dans l'ordre du parcours : session → présence → dépôt → assignation → relecture → tableau | `[JALON] v0.1` |
| 3. Enveloppe | Issues séparées pour le bug et l'évolution avant tout code ; re-priorisation écrite ; test qui reproduit le bug puis correctif ; nouvelle migration et contrat mis à jour ; ce document et les diagrammes corrigés | PR du correctif et PR de l'évolution fusionnées |
| 4. Version 1.0 | Stories Should restantes (EF7 à EF10), puis EF11 si le temps le permet ; CHANGELOG ; README testé depuis un clone vierge | `[JALON] v1.0` |
| 5. Épreuve Git | Second dépôt, cinq situations du README, `push --all` | Push de toutes les branches |
| 6. Soumission | Vérification des deux liens en navigation privée, relevé des hash complets | `SOUMISSION.md` déposé avant 17h30 |

**Si je prends du retard :** je sacrifie dans cet ordre EF11 (Could), puis EF10, EF9, EF8, EF7 (Should), en le notant dans le journal et dans les issues concernées. Je ne sacrifie jamais les jalons, les tests B6, le README de démarrage, l'étape 3 ni la soumission. Je vise le jalon v0.1 à mi-journée au plus tard, pour garder du temps pour l'enveloppe.

**Definition of Done — un ticket est terminé quand :**
- ses critères d'acceptation sont couverts par des tests automatisés qui passent, nommés d'après les RG concernées ;
- la CI est verte sur sa PR, qui cite l'issue (`Closes #n`), les EF et les RG ;
- le contrat d'API et les migrations sont à jour si le ticket les touche (jamais de migration existante modifiée) ;
- aucun secret ni fichier généré n'est commité ;
- la PR est fusionnée dans `main` par un merge commit, ce qui ferme l'issue ;
- la documentation touchée (README, CDC, diagrammes, CHANGELOG) est à jour.

---

## Journal des révisions

| Version | Quand | Ce qui a changé et pourquoi |
|---|---|---|
| 1 | 25/09/2026, étape 1 | Version initiale |
