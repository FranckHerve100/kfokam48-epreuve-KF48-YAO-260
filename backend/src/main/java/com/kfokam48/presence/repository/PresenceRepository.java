package com.kfokam48.presence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kfokam48.presence.domain.Presence;

public interface PresenceRepository extends JpaRepository<Presence, Long> {
}
