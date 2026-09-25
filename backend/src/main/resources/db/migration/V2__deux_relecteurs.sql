-- V2 (étape 3, changement de besoin) : chaque exercice est relu par deux pairs différents (RG9 v2).
-- V1 n'est pas modifiée. Les données existantes restent valides : une relecture par exercice satisfait
-- la nouvelle unicité, et les statuts existants restent autorisés.

-- L'index sur exercice_id remplace l'unicité de V1 (clé étrangère, tirage, tableau)
CREATE INDEX idx_relecture_exercice ON relecture (exercice_id);

-- Deux relecteurs différents par exercice, au lieu d'un seul
ALTER TABLE relecture DROP CONSTRAINT uk_relecture_exercice;
ALTER TABLE relecture ADD CONSTRAINT uk_relecture_exercice_relecteur UNIQUE (exercice_id, relecteur_id);

-- Nouveau statut : une note rendue sur deux, note provisoire (D4)
ALTER TABLE exercice DROP CONSTRAINT ck_exercice_statut;
ALTER TABLE exercice ADD CONSTRAINT ck_exercice_statut
    CHECK (statut IN ('DEPOSE', 'EN_ATTENTE_RELECTURE', 'PARTIELLEMENT_RELU', 'RELU'));
