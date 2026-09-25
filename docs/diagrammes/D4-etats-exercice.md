# D4 — États-transitions du cycle de vie d'un exercice

```mermaid
stateDiagram-v2
    [*] --> DEPOSE: POST /api/exercices<br/>aucun relecteur éligible (H1)
    [*] --> EN_ATTENTE_RELECTURE: POST /api/exercices<br/>relecteur tiré (RG10)

    DEPOSE --> EN_ATTENTE_RELECTURE: nouvelle présence sur la session<br/>→ tirage relancé (H1)
    DEPOSE --> DEPOSE: PUT /api/exercices/{id}<br/>nouveau lien (RG8)
    EN_ATTENTE_RELECTURE --> EN_ATTENTE_RELECTURE: PUT /api/exercices/{id}<br/>nouveau lien (RG8)

    EN_ATTENTE_RELECTURE --> RELU: POST /api/relectures/{id}<br/>note 0–20 (RG11)
    RELU --> [*]

    note right of RELU
        Note définitive (RG12, Q15).
        Tout nouvel envoi → 409.
        Remplacement du lien → 409.
    end note

    note right of EN_ATTENTE_RELECTURE
        Peut durer indéfiniment,
        même après la clôture (RG14, H9).
    end note
```

| Transition | Déclencheur | Refusée si | Code |
|---|---|---|---|
| → `DEPOSE` ou `EN_ATTENTE_RELECTURE` | Dépôt | Session clôturée ; déjà déposé ; lien invalide ; session ou étudiant inconnu ; étudiant hors promotion | 409 / 409 / 400 / 400 / 400 |
| `DEPOSE` → `EN_ATTENTE_RELECTURE` | Nouvelle présence sur la session | — | — |
| Remplacement du lien | `PUT /api/exercices/{id}` | Session clôturée ; état `RELU` | 409 |
| `EN_ATTENTE_RELECTURE` → `RELU` | Relecture rendue | Note invalide ou relecture inconnue ; relecteur = auteur ou non assigné ; déjà rendue | 400 / 403 / 409 |
