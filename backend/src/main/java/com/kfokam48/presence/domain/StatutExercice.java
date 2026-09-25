package com.kfokam48.presence.domain;

/** Cycle de vie d'un exercice (diagramme D4). */
public enum StatutExercice {
    /** Déposé, aucun relecteur éligible pour l'instant (H1). */
    DEPOSE,
    /** Relecteur tiré, note pas encore rendue (RG14). */
    EN_ATTENTE_RELECTURE,
    /** Note rendue, définitive (RG12). */
    RELU
}
