package com.kfokam48.presence.repository;

/**
 * Ligne du tableau, produite par la requête agrégée de {@link EtudiantRepository#tableau(Long)}.
 * {@code moyenne} : moyenne des notes retenues (RG15 v2) ; {@code notesProvisoires} : exercices dont la note
 * retenue ne repose encore que sur une relecture sur deux.
 */
public record LigneTableau(Long etudiantId, String nom, Long presences, Long exercicesDeposes, Double moyenne,
                           Long relecturesEnAttente, Long notesProvisoires) {
}
