package com.kfokam48.presence.domain;

/** Cycle de vie d'un exercice (diagramme D4). */
public enum StatutExercice {
    /** Déposé, aucun relecteur éligible pour l'instant (H1). */
    DEPOSE,
    /** Au moins un relecteur tiré, aucune note rendue (RG14). */
    EN_ATTENTE_RELECTURE,
    /** Une note rendue sur deux : note retenue provisoire (RG15 v2, H12). */
    PARTIELLEMENT_RELU,
    /** Notes rendues par les relecteurs, définitives (RG12) ; note retenue = leur moyenne. */
    RELU
}
