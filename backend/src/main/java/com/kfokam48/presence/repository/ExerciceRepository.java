package com.kfokam48.presence.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kfokam48.presence.domain.Exercice;
import com.kfokam48.presence.domain.StatutExercice;

public interface ExerciceRepository extends JpaRepository<Exercice, Long> {

    boolean existsBySessionIdAndEtudiantId(Long sessionId, Long etudiantId);

    List<Exercice> findBySessionIdAndStatutInOrderByDeposeAtAsc(Long sessionId, Collection<StatutExercice> statuts);
}
