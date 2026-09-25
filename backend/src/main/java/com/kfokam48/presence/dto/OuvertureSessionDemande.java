package com.kfokam48.presence.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Corps de POST /api/sessions (contrat imposé). */
public record OuvertureSessionDemande(
        @NotBlank(message = "CHAMP_MANQUANT") @Size(max = 200) String titre,
        @NotNull(message = "CHAMP_MANQUANT") Long promotionId) {
}
