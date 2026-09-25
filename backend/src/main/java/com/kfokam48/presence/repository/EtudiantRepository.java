package com.kfokam48.presence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kfokam48.presence.domain.Etudiant;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    List<Etudiant> findByPromotionIdOrderByNomAsc(Long promotionId);
}
