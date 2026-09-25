package com.kfokam48.presence.dto;

import com.kfokam48.presence.domain.StatutExercice;

/** Réponse 201 de POST /api/exercices (contrat imposé) : statut selon le diagramme D4. */
public record ExerciceDeposeDto(Long id, StatutExercice statut) {
}
