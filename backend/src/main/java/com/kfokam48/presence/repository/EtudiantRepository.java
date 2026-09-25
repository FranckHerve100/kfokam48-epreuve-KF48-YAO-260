package com.kfokam48.presence.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.kfokam48.presence.domain.Etudiant;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {
}
