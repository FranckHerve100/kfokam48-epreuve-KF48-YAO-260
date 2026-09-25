package com.kfokam48.presence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corps de POST /api/exercices (contrat imposé). */
public record DepotExerciceDemande(
        @NotNull(message = "CHAMP_MANQUANT") Long sessionId,
        @NotNull(message = "CHAMP_MANQUANT") Long etudiantId,
        @NotBlank(message = "CHAMP_MANQUANT") String lien) {
}
