package com.kfokam48.presence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kfokam48.presence.domain.Relecture;

public interface RelectureRepository extends JpaRepository<Relecture, Long> {

    /** Charge d'un relecteur : relectures qui lui sont assignées dans la session (RG10). */
    long countByRelecteurIdAndExerciceSessionId(Long relecteurId, Long sessionId);

    List<Relecture> findByRelecteurIdOrderByAssigneeAtDesc(Long relecteurId);
}
