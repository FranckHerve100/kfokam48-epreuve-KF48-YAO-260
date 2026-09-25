package com.kfokam48.presence.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.presence.domain.Relecture;
import com.kfokam48.presence.dto.RelectureAssigneeDto;
import com.kfokam48.presence.dto.RelectureDemande;
import com.kfokam48.presence.dto.StatutRelecture;
import com.kfokam48.presence.exception.AutoRelectureException;
import com.kfokam48.presence.exception.EtudiantInconnuException;
import com.kfokam48.presence.exception.RelecteurNonAssigneException;
import com.kfokam48.presence.exception.RelectureDejaRendueException;
import com.kfokam48.presence.exception.RelectureInconnueException;
import com.kfokam48.presence.repository.EtudiantRepository;
import com.kfokam48.presence.repository.RelectureRepository;

/** Rendu des relectures (EF5) et liste des relectures d'un étudiant (écran relecteur). */
@Service
public class RelectureService {

    private final RelectureRepository relectures;
    private final EtudiantRepository etudiants;
    private final Clock horloge;

    public RelectureService(RelectureRepository relectures, EtudiantRepository etudiants, Clock horloge) {
        this.relectures = relectures;
        this.etudiants = etudiants;
        this.horloge = horloge;
    }

    /**
     * Ordre : relecture connue → si l'en-tête X-Etudiant-Id est présent (H11) : pas l'auteur (RG10),
     * puis le relecteur tiré → pas déjà rendue (RG12, Q15). La note (RG11) est validée avant, sur le DTO.
     */
    @Transactional
    public void rendre(Long relectureId, RelectureDemande demande, Long etudiantId) {
        Relecture relecture = relectures.findById(relectureId).orElseThrow(RelectureInconnueException::new);
        if (etudiantId != null) {
            if (etudiantId.equals(relecture.getExercice().getEtudiant().getId())) {
                throw new AutoRelectureException();
            }
            if (!etudiantId.equals(relecture.getRelecteur().getId())) {
                throw new RelecteurNonAssigneException();
            }
        }
        if (relecture.estRendue()) {
            throw new RelectureDejaRendueException();
        }
        relecture.rendre(demande.note(), demande.commentaire(), Instant.now(horloge).truncatedTo(ChronoUnit.SECONDS));
        relecture.getExercice().marquerRelu();
    }

    /** Relectures assignées à un étudiant, en attente d'abord puis les plus récentes (404 si l'étudiant n'existe pas). */
    @Transactional(readOnly = true)
    public List<RelectureAssigneeDto> relecturesDe(Long etudiantId) {
        if (!etudiants.existsById(etudiantId)) {
            throw EtudiantInconnuException.enChemin();
        }
        return relectures.findByRelecteurIdOrderByAssigneeAtDesc(etudiantId).stream()
                .sorted(Comparator.comparing(Relecture::estRendue))
                .map(r -> new RelectureAssigneeDto(r.getId(), r.getExercice().getId(), r.getExercice().getLien(),
                        r.estRendue() ? StatutRelecture.RENDUE : StatutRelecture.EN_ATTENTE))
                .toList();
    }
}
