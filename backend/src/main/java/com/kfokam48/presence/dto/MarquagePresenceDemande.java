package com.kfokam48.presence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Corps de POST /api/presences (contrat imposé). */
public record MarquagePresenceDemande(
        @NotBlank(message = "CHAMP_MANQUANT") String code,
        @NotNull(message = "CHAMP_MANQUANT") Long etudiantId) {
}
