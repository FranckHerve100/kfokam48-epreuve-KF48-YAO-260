# D3 — Séquence : marquer sa présence

`POST /api/presences { code, etudiantId }`. Chaque branche correspond à un code HTTP **du contrat** `api/contrat.yaml` : 201, 400, 409 ou 410, et aucun autre, car l'opération est imposée. Les cas ajoutés par le cahier des charges réutilisent ces statuts et se distinguent par la valeur de `code`. Les contrôles s'exécutent **dans cet ordre** : c'est l'ordre que suivent le service et ses tests.

```mermaid
sequenceDiagram
    autonumber
    actor E as Étudiant
    participant F as Front (api/client.ts)
    participant C as PresenceController
    participant S as PresenceService
    participant R as Repositories
    participant A as AssignationService

    E->>F: choisit son nom, saisit le code
    F->>C: POST /api/presences { code, etudiantId }
    C->>C: validation du DTO
    alt code ou etudiantId absent (RG17)
        C-->>F: 400 { code: "CHAMP_MANQUANT" }
    else DTO valide
        C->>S: marquer(code, etudiantId)
        S->>R: findEtudiant(etudiantId)
        alt étudiant inexistant
            S-->>C: EtudiantInconnuException
            C-->>F: 400 { code: "ETUDIANT_INCONNU" }
        else étudiant bloqué (RG4, étape 4)
            S-->>C: CodeBloqueException
            C-->>F: 400 { code: "CODE_BLOQUE" }
        else étudiant connu, non bloqué
            S->>R: findSessionNonExpiree(code) puis findDerniereSession(code)
            alt aucune session avec ce code (RG3)
                S->>R: incrémenter erreurs_code, bloquer à 5 (RG4, étape 4)
                S-->>C: CodeInconnuException
                C-->>F: 400 { code: "CODE_INCONNU" }
            else code expiré : maintenant > expirationAt, ou session clôturée (RG1, H2)
                S-->>C: CodeExpireException
                C-->>F: 410 { code: "CODE_EXPIRE" }
            else étudiant d'une autre promotion (RG18)
                S-->>C: HorsPromotionException
                C-->>F: 400 { code: "HORS_PROMOTION" }
            else déjà présent (RG2)
                S-->>C: DejaPresentException
                C-->>F: 409 { code: "DEJA_PRESENT" }
            else cas nominal
                S->>R: save(Presence, source = ETUDIANT)
                S->>R: remettre erreurs_code à 0 (RG4, étape 4)
                S->>A: relancerTirage(sessionId) (H1)
                S-->>C: Presence
                C-->>F: 201 { id, sessionId, etudiantId, source }
            end
        end
    end
    F-->>E: message de succès ou d'erreur
```

| Branche | HTTP | `code` | Règle | Test d'intégration attendu |
|---|---|---|---|---|
| Champ manquant | 400 | `CHAMP_MANQUANT` | RG17 | `RG17_presenceSansCode_renvoie400` |
| Étudiant inexistant | 400 | `ETUDIANT_INCONNU` | EF2 | `presence_etudiantInconnu_renvoie400` |
| Étudiant bloqué | 400 | `CODE_BLOQUE` | RG4 (étape 4) | `RG4_apres5Erreurs_renvoie400CodeBloque` |
| Code inconnu | 400 | `CODE_INCONNU` | RG3 | `RG3_codeInconnu_renvoie400` |
| Code expiré ou session clôturée | 410 | `CODE_EXPIRE` | RG1, H2 | `RG1_codeExpireApres15Minutes_renvoie410` |
| Hors promotion | 400 | `HORS_PROMOTION` | RG18 | `RG18_etudiantAutrePromotion_renvoie400` |
| Déjà présent | 409 | `DEJA_PRESENT` | RG2 | `RG2_dejaPresent_renvoie409` |
| Nominal | 201 | — | EF2 | `EF2_codeValide_renvoie201SourceEtudiant` |

Le blocage après 5 codes erronés (RG4) est une story Should (EF8) : il est placé dans la séquence pour fixer l'ordre des contrôles, mais il est livré à l'étape 4. Les colonnes `erreurs_code` et `bloque_jusqu_a` existent dès `V1` (D2).
