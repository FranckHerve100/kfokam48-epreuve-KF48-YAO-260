package com.kfokam48.presence.repository;

/** Ligne brute du tableau, produite par la requête agrégée de {@link EtudiantRepository#tableau(Long)}. */
public record LigneTableau(Long etudiantId, String nom, Long presences, Long exercicesDeposes, Double moyenne,
                           Long relecturesEnAttente) {
}
