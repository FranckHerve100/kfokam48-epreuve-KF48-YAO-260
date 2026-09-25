package com.kfokam48.presence.dto;

/** Relecture assignée à un étudiant : l'exercice à relire et son lien (écran relecteur). */
public record RelectureAssigneeDto(Long id, Long exerciceId, String lien, StatutRelecture statut) {
}
