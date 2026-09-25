package com.kfokam48.presence.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.presence.domain.Etudiant;
import com.kfokam48.presence.domain.Exercice;
import com.kfokam48.presence.domain.ExerciceDepose;
import com.kfokam48.presence.domain.Presence;
import com.kfokam48.presence.domain.PresenceEnregistree;
import com.kfokam48.presence.domain.Relecture;
import com.kfokam48.presence.domain.Session;
import com.kfokam48.presence.domain.StatutExercice;
import com.kfokam48.presence.repository.ExerciceRepository;
import com.kfokam48.presence.repository.PresenceRepository;
import com.kfokam48.presence.repository.RelectureRepository;

/**
 * Tirage du relecteur (EF4) : un seul par exercice (RG9), parmi les étudiants présents à la session
 * sauf l'auteur ; le moins chargé en relectures de la session d'abord, puis au hasard entre ex æquo (RG10).
 * Sans candidat, l'exercice reste DEPOSE et le tirage est relancé à chaque nouvelle présence (H1).
 * Déclenché par les événements de domaine publiés au save : dans la transaction du dépôt pour un exercice,
 * après validation et dans une transaction à part pour une présence (#50).
 */
@Service
public class AssignationService {

    private final PresenceRepository presences;
    private final RelectureRepository relectures;
    private final ExerciceRepository exercices;
    private final RandomGenerator hasard;
    private final Clock horloge;

    public AssignationService(PresenceRepository presences, RelectureRepository relectures, ExerciceRepository exercices,
                              RandomGenerator hasard, Clock horloge) {
        this.presences = presences;
        this.relectures = relectures;
        this.exercices = exercices;
        this.hasard = hasard;
        this.horloge = horloge;
    }

    @EventListener
    @Transactional
    public void exerciceDepose(ExerciceDepose evenement) {
        assigner(evenement.exercice());
    }

    /**
     * Relance déclenchée par une nouvelle présence (H1), dans une transaction à part : appelée par
     * {@link RelanceTirageApresPresence} une fois la présence validée, pour qu'un conflit de tirage ne puisse
     * jamais annuler une présence (#50).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void presenceEnregistree(PresenceEnregistree evenement) {
        relancerTirage(evenement.presence().getSession());
    }

    /** Relance le tirage de tous les exercices de la session restés sans relecteur (H1). */
    @Transactional
    public void relancerTirage(Session session) {
        exercices.findBySessionIdAndStatutOrderByDeposeAtAsc(session.getId(), StatutExercice.DEPOSE)
                .forEach(this::assigner);
    }

    @Transactional
    public void assigner(Exercice exercice) {
        if (exercice.getStatut() != StatutExercice.DEPOSE) {
            return;
        }
        Long sessionId = exercice.getSession().getId();
        List<Etudiant> candidats = presences.findBySessionId(sessionId).stream()
                .map(Presence::getEtudiant)
                .filter(etudiant -> !etudiant.getId().equals(exercice.getEtudiant().getId()))
                .toList();
        if (candidats.isEmpty()) {
            return;
        }
        Etudiant relecteur = moinsCharge(candidats, sessionId);
        relectures.save(new Relecture(exercice, relecteur, Instant.now(horloge).truncatedTo(ChronoUnit.SECONDS)));
        exercice.passerEnAttenteDeRelecture();
    }

    private Etudiant moinsCharge(List<Etudiant> candidats, Long sessionId) {
        Map<Long, List<Etudiant>> parCharge = candidats.stream().collect(Collectors.groupingBy(
                etudiant -> relectures.countByRelecteurIdAndExerciceSessionId(etudiant.getId(), sessionId)));
        List<Etudiant> exAequo = parCharge.entrySet().stream()
                .min(Comparator.comparingLong(Map.Entry::getKey))
                .orElseThrow()
                .getValue();
        return exAequo.get(hasard.nextInt(exAequo.size()));
    }
}
