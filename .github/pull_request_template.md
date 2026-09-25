## Quoi
Closes #<n°>

## Exigences et règles
- EFx : 
- RGx : 

## Comment vérifier
1. `./mvnw test` ou `npm test`
2. Requête / écran à tester : 

## Checklist
- [ ] Commits atomiques, messages explicites
- [ ] Tests ajoutés (nom du test cite la RG)
- [ ] Contrat `api/contrat.yaml` inchangé ou mis à jour dans cette PR
- [ ] Migration Flyway ajoutée si le schéma change (jamais modifier une migration existante)
- [ ] Aucun secret, aucun fichier généré
- [ ] CHANGELOG / docs mis à jour si besoin