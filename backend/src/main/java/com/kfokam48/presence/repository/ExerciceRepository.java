package com.kfokam48.presence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kfokam48.presence.domain.Exercice;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);
}
