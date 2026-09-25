package com.kfokam48.presence.dto;

import java.math.BigDecimal;

/** Ligne de GET /api/tableau (contrat imposé) ; moyenne à 2 décimales, null sans note reçue (RG15). */
public record LigneTableauDto(Long etudiantId, String nom, int presences, int exercicesDeposes, BigDecimal moyenne,
                              int relecturesEnAttente) {
}
