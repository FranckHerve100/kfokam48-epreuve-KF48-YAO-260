# D1 — Cas d'utilisation

Le relecteur n'est pas un acteur distinct : c'est un étudiant à qui le système a assigné un exercice (CDC §2). Il apparaît donc comme un rôle de l'étudiant, relié par une flèche en pointillés.

```mermaid
flowchart LR
    F(["👤 Formateur"])
    E(["👤 Étudiant"])
    R(["👤 Relecteur<br/>(étudiant assigné)"])
    S(["⚙️ Système"])

    subgraph APP["Application KFOKAM48"]
        UC1(["EF1 Ouvrir une session<br/>et obtenir un code"])
        UC7(["EF7 Ajouter une présence<br/>à la main"])
        UC9(["EF9 Clôturer une session"])
        UC6(["EF6 Consulter le tableau<br/>de la promotion"])

        UC0(["Se choisir dans la liste<br/>(sans mot de passe)"])
        UC2(["EF2 Marquer sa présence"])
        UC3(["EF3 Déposer le lien<br/>de son exercice"])
        UC11(["EF11 Remplacer le lien"])
        UC10(["EF10 Voir sa note<br/>et le commentaire"])

        UC5(["EF5 Noter et commenter<br/>un exercice assigné"])

        UC4(["EF4 Tirer un relecteur<br/>au hasard"])
        UC8(["EF8 Bloquer après<br/>5 codes erronés"])
        UCX(["Expirer le code<br/>après 15 min"])
    end

    F --- UC1
    F --- UC7
    F --- UC9
    F --- UC6

    E --- UC0
    E --- UC2
    E --- UC3
    E --- UC11
    E --- UC10
    E -. devient .-> R
    R --- UC5

    S --- UC4
    S --- UC8
    S --- UCX

    UC3 -. déclenche .-> UC4
    UC2 -. peut déclencher .-> UC4
    UC2 -. vérifie .-> UC8
```

| Acteur | Cas d'utilisation | Règles |
|---|---|---|
| Formateur | EF1, EF6, EF7, EF9 | RG1, RG5, RG14, RG15 |
| Étudiant | EF2, EF3, EF10, EF11 | RG1–RG4, RG6–RG8, RG13, RG16, RG18 |
| Relecteur | EF5 | RG10, RG11, RG12 |
| Système | EF4, EF8, expiration | RG1, RG4, RG9, RG10 |

`EF2 peut déclencher EF4` : une nouvelle présence relance le tirage des exercices restés sans relecteur (CDC §7, H1).
