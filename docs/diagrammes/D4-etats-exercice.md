# D4 — États-transitions du cycle de vie d'un exercice

*v2 (étape 3, changement de besoin) : chaque exercice est relu par deux pairs. Le statut `PARTIELLEMENT_RELU` porte la note provisoire, en attendant la seconde relecture.*

```mermaid
stateDiagram-v2
    [*] --> DEPOSE: POST /api/exercices<br/>aucun relecteur éligible (H1)
    [*] --> EN_ATTENTE_RELECTURE: POST /api/exercices<br/>1 ou 2 relecteurs tirés (RG10)

    DEPOSE --> EN_ATTENTE_RELECTURE: nouvelle présence sur la session<br/>→ tirage relancé (H1)
    EN_ATTENTE_RELECTURE --> EN_ATTENTE_RELECTURE: nouvelle présence<br/>→ second relecteur tiré s'il manque
    DEPOSE --> DEPOSE: PUT /api/exercices/{id}<br/>nouveau lien (RG8)
    EN_ATTENTE_RELECTURE --> EN_ATTENTE_RELECTURE: PUT /api/exercices/{id}<br/>nouveau lien (RG8)

    EN_ATTENTE_RELECTURE --> PARTIELLEMENT_RELU: POST /api/relectures/{id}<br/>première note 0–20 (RG11)
    PARTIELLEMENT_RELU --> RELU: POST /api/relectures/{id}<br/>seconde note (RG11)
    RELU --> [*]

    note right of PARTIELLEMENT_RELU
        Note retenue = la seule note rendue,
        affichée comme provisoire (RG15, H12).
    end note

    note right of RELU
        Note retenue = moyenne des deux notes.
        Notes définitives (RG12, Q15) : tout nouvel envoi → 409.
    end note

    note right of EN_ATTENTE_RELECTURE
        Peut durer indéfiniment,
        même après la clôture (RG14, H9).
    end note
```

| Transition | Déclencheur | Refusée si | Code |
|---|---|---|---|
| → `DEPOSE` ou `EN_ATTENTE_RELECTURE` | Dépôt | Session clôturée ; déjà déposé ; lien invalide ; session ou étudiant inconnu ; étudiant hors promotion | 409 / 409 / 400 / 400 / 400 |
| `DEPOSE` → `EN_ATTENTE_RELECTURE`, ou second relecteur | Nouvelle présence sur la session (tirage après validation de la présence, #50) | — | — |
| Remplacement du lien | `PUT /api/exercices/{id}` | Session clôturée ; une relecture rendue | 409 |
| `EN_ATTENTE_RELECTURE` → `PARTIELLEMENT_RELU` → `RELU` | Relectures rendues, une par relecteur | Note invalide ou relecture inconnue ; relecteur = auteur ou non assigné ; déjà rendue | 400 / 403 / 409 |

Données antérieures à la migration V2 : un exercice déjà `RELU` avec un seul relecteur reste `RELU` (note définitive, RG12) ; un exercice `EN_ATTENTE_RELECTURE` avec un seul relecteur reçoit son second relecteur à la prochaine présence.
