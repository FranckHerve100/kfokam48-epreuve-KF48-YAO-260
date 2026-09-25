package com.kfokam48.presence.dto;

import com.kfokam48.presence.domain.SourcePresence;

/** Réponse 201 de POST /api/presences (contrat imposé). */
public record PresenceDto(Long id, Long sessionId, Long etudiantId, SourcePresence source) {
}
