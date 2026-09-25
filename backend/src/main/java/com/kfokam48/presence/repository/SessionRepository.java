package com.kfokam48.presence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kfokam48.presence.domain.Session;

public interface SessionRepository extends JpaRepository<Session, Long> {

    /** Un code est pris tant qu'une session qui le porte n'a pas expiré (H5). */
    boolean existsByCodeAndExpirationAtGreaterThanEqual(String code, Instant instant);

    List<Session> findByPromotionIdOrderByOuvertureAtDesc(Long promotionId);

    /** Seule la session la plus récente portant un code peut être en cours (H5). */
    Optional<Session> findFirstByCodeOrderByOuvertureAtDesc(String code);
}
