package com.kfokam48.presence.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corps de POST /api/relectures/{id} (contrat imposé). La note est un entier de 0 à 20 (RG11) :
 * une valeur décimale ou non numérique est refusée à la lecture du JSON, aussi en NOTE_INVALIDE.
 */
public record RelectureDemande(
        @NotNull(message = "CHAMP_MANQUANT") @Min(value = 0, message = "NOTE_INVALIDE") @Max(value = 20, message = "NOTE_INVALIDE") Integer note,
        @NotNull(message = "CHAMP_MANQUANT") @Size(max = 2000) String commentaire) {
}
