package com.kfokam48.presence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kfokam48.presence.domain.Session;

public interface SessionRepository extends JpaRepository<Session, Long> {
}
