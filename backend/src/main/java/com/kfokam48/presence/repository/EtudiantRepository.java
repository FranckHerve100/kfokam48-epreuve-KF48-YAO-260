package com.kfokam48.presence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kfokam48.presence.domain.Etudiant;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    List<Etudiant> findByPromotionIdOrderByNomAsc(Long promotionId);

    /**
     * Tableau du formateur en UNE requête (ENF2) : par étudiant de la promotion, ses présences, ses exercices
     * déposés, la moyenne des notes reçues (RG15, null sans note) et les relectures qu'il doit encore (RG14).
     */
    @Query("""
            select new com.kfokam48.presence.repository.LigneTableau(
                e.id,
                e.nom,
                (select count(p) from Presence p where p.etudiant = e),
                (select count(x) from Exercice x where x.etudiant = e),
                (select avg(r.note) from Relecture r where r.exercice.etudiant = e and r.rendueAt is not null),
                (select count(d) from Relecture d where d.relecteur = e and d.rendueAt is null))
            from Etudiant e
            where e.promotion.id = :promotionId
            order by e.nom
            """)
    List<LigneTableau> tableau(@Param("promotionId") Long promotionId);
}
