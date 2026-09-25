package com.kfokam48.presence.dto;

import java.time.Instant;

/** Réponse 201 de POST /api/sessions (contrat imposé). */
public record SessionOuverteDto(Long id, String code, Instant ouvertureAt, Instant expirationAt) {
}
