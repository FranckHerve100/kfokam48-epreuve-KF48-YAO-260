package com.kfokam48.presence.dto;

import java.math.BigDecimal;

/**
 * Ligne de GET /api/tableau (contrat imposé) ; moyenne des notes retenues à 2 décimales, null sans note (RG15) ;
 * moyenneProvisoire : ajout v2, vrai si une note retenue ne repose encore que sur une relecture (H12).
 */
public record LigneTableauDto(Long etudiantId, String nom, int presences, int exercicesDeposes, BigDecimal moyenne,
                              int relecturesEnAttente, boolean moyenneProvisoire) {
}
