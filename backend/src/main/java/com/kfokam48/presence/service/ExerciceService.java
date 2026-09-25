package com.kfokam48.presence.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.presence.domain.Etudiant;
import com.kfokam48.presence.domain.Exercice;
import com.kfokam48.presence.domain.Session;
import com.kfokam48.presence.dto.DepotExerciceDemande;
import com.kfokam48.presence.dto.ExerciceDeposeDto;
import com.kfokam48.presence.exception.EtudiantInconnuException;
import com.kfokam48.presence.exception.ExerciceDejaDeposeException;
import com.kfokam48.presence.exception.HorsPromotionException;
import com.kfokam48.presence.exception.LienInvalideException;
import com.kfokam48.presence.exception.SessionClotureeException;
import com.kfokam48.presence.exception.SessionInconnueException;
import com.kfokam48.presence.repository.EtudiantRepository;
import com.kfokam48.presence.repository.ExerciceRepository;
import com.kfokam48.presence.repository.SessionRepository;

/** Dépôt du lien d'un exercice (EF3), statut initial selon le diagramme D4. */
@Service
public class ExerciceService {

    private final SessionRepository sessions;
    private final EtudiantRepository etudiants;
    private final ExerciceRepository exercices;
    private final Clock horloge;

    public ExerciceService(SessionRepository sessions, EtudiantRepository etudiants, ExerciceRepository exercices,
                           Clock horloge) {
        this.sessions = sessions;
        this.etudiants = etudiants;
        this.exercices = exercices;
        this.horloge = horloge;
    }

    /**
     * Ordre : lien valide (RG16) → session connue → étudiant connu → même promotion (RG18)
     * → session non clôturée (RG7, le code peut être expiré) → premier dépôt (RG6).
     * Le dépôt ne dépend pas de la présence (H8).
     */
    @Transactional
    public ExerciceDeposeDto deposer(DepotExerciceDemande demande) {
        if (!ValidateurLien.estValide(demande.lien())) {
            throw new LienInvalideException();
        }
        Session session = sessions.findById(demande.sessionId()).orElseThrow(SessionInconnueException::enCorps);
        Etudiant auteur = etudiants.findById(demande.etudiantId()).orElseThrow(EtudiantInconnuException::enCorps);
        if (!auteur.appartientA(session.getPromotion())) {
            throw new HorsPromotionException();
        }
        if (session.estCloturee()) {
            throw new SessionClotureeException();
        }
        if (exercices.existsBySessionIdAndEtudiantId(session.getId(), auteur.getId())) {
            throw new ExerciceDejaDeposeException();
        }
        Instant maintenant = Instant.now(horloge).truncatedTo(ChronoUnit.SECONDS);
        Exercice exercice = enregistrer(new Exercice(session, auteur, demande.lien().strip(), maintenant));
        return new ExerciceDeposeDto(exercice.getId(), exercice.getStatut());
    }

    /** La contrainte d'unicité (session, étudiant) tranche un double dépôt simultané (RG6). */
    private Exercice enregistrer(Exercice exercice) {
        try {
            return exercices.saveAndFlush(exercice);
        } catch (DataIntegrityViolationException doublon) {
            throw new ExerciceDejaDeposeException();
        }
    }
}
