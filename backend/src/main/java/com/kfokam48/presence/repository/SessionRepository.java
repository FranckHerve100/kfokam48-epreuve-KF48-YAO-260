package com.kfokam48.presence.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kfokam48.presence.domain.Session;

public interface SessionRepository extends JpaRepository<Session, Long> {

    /** Un code est pris tant qu'une session qui le porte n'a pas expiré (H5). */
    boolean existsByCodeAndExpirationAtGreaterThanEqual(String code, Instant instant);
}
