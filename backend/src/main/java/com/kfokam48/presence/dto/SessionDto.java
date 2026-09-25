package com.kfokam48.presence.dto;

import java.time.Instant;

/** Session d'une promotion ; clotureAt vaut null tant qu'elle n'est pas clôturée (H2). */
public record SessionDto(Long id, String titre, String code, Instant ouvertureAt, Instant expirationAt,
                         Instant clotureAt) {
}
