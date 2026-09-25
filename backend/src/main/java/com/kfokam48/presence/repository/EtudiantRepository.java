package com.kfokam48.presence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.kfokam48.presence.domain.Etudiant;

public interface EtudiantRepository extends JpaRepository<Etudiant, Long> {

    List<Etudiant> findByPromotionIdOrderByNomAsc(Long promotionId);

    /**
     * Tableau du formateur en UNE requête (ENF2) : par étudiant de la promotion, ses présences, ses exercices,
     * la moyenne de ses notes retenues (RG15 v2 : note retenue d'un exercice = moyenne de ses notes rendues),
     * les relectures qu'il doit encore (RG14) et ses exercices à note provisoire (PARTIELLEMENT_RELU, H12).
     * Requête native : une moyenne de moyennes par exercice ne s'exprime pas en JPQL.
     */
    @Query(value = """
            select e.id as etudiantId,
                   e.nom as nom,
                   (select count(*) from presence p where p.etudiant_id = e.id) as presences,
                   (select count(*) from exercice x where x.etudiant_id = e.id) as exercicesDeposes,
                   (select avg(n.note_retenue)
                      from (select avg(cast(r.note as double precision)) as note_retenue
                              from relecture r join exercice x on x.id = r.exercice_id
                             where x.etudiant_id = e.id and r.rendue_at is not null
                             group by x.id) n) as moyenne,
                   (select count(*) from relecture d where d.relecteur_id = e.id and d.rendue_at is null) as relecturesEnAttente,
                   (select count(*) from exercice x where x.etudiant_id = e.id and x.statut = 'PARTIELLEMENT_RELU') as notesProvisoires
              from etudiant e
             where e.promotion_id = :promotionId
             order by e.nom
            """, nativeQuery = true)
    List<LigneBrute> tableauBrut(@Param("promotionId") Long promotionId);

    default List<LigneTableau> tableau(Long promotionId) {
        return tableauBrut(promotionId).stream()
                .map(l -> new LigneTableau(l.getEtudiantId().longValue(), l.getNom(), l.getPresences().longValue(),
                        l.getExercicesDeposes().longValue(), l.getMoyenne() == null ? null : l.getMoyenne().doubleValue(),
                        l.getRelecturesEnAttente().longValue(), l.getNotesProvisoires().longValue()))
                .toList();
    }

    /** Projection des colonnes de la requête native (types numériques variables selon la base). */
    interface LigneBrute {
        Number getEtudiantId();

        String getNom();

        Number getPresences();

        Number getExercicesDeposes();

        Number getMoyenne();

        Number getRelecturesEnAttente();

        Number getNotesProvisoires();
    }
}
