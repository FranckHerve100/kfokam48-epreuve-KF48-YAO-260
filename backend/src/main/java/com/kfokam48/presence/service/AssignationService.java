package com.kfokam48.presence.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
 * Tirage des relecteurs (EF4) : deux relecteurs différents par exercice (RG9 v2, #52), parmi les étudiants
 * présents à la session sauf l'auteur ; les moins chargés en relectures de la session d'abord, puis au hasard
 * entre ex æquo (RG10). Sans candidat, l'exercice reste DEPOSE ; les relecteurs manquants sont tirés à chaque
 * nouvelle présence (H1).
 * Déclenché par les événements de domaine publiés au save : dans la transaction du dépôt pour un exercice,
 * après validation et dans une transaction à part pour une présence (#50).
 */
@Service
public class AssignationService {

    /** Deux relecteurs différents par exercice (RG9, changement de besoin de l'étape 3, #52). */
    static final int RELECTEURS_PAR_EXERCICE = 2;

    /** Exercices auxquels un relecteur peut encore manquer. */
    static final Set<StatutExercice> STATUTS_A_COMPLETER =
            EnumSet.of(StatutExercice.DEPOSE, StatutExercice.EN_ATTENTE_RELECTURE, StatutExercice.PARTIELLEMENT_RELU);

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

    /** Relance le tirage des exercices de la session auxquels il manque encore un relecteur (H1, RG9 v2). */
    @Transactional
    public void relancerTirage(Session session) {
        exercices.findBySessionIdAndStatutInOrderByDeposeAtAsc(session.getId(), STATUTS_A_COMPLETER)
                .forEach(this::assigner);
    }

    /** Complète l'exercice jusqu'à deux relecteurs différents (RG9 v2), parmi les présents sauf l'auteur (RG10). */
    @Transactional
    public void assigner(Exercice exercice) {
        if (exercice.getStatut() == StatutExercice.RELU) {
            return;
        }
        List<Long> dejaAssignes = relectures.findByExerciceId(exercice.getId()).stream()
                .map(relecture -> relecture.getRelecteur().getId())
                .toList();
        int manquants = RELECTEURS_PAR_EXERCICE - dejaAssignes.size();
        if (manquants <= 0) {
            return;
        }
        Long sessionId = exercice.getSession().getId();
        List<Etudiant> candidats = new ArrayList<>(presences.findBySessionId(sessionId).stream()
                .map(Presence::getEtudiant)
                .filter(etudiant -> !etudiant.getId().equals(exercice.getEtudiant().getId()))
                .filter(etudiant -> !dejaAssignes.contains(etudiant.getId()))
                .toList());
        Instant maintenant = Instant.now(horloge).truncatedTo(ChronoUnit.SECONDS);
        for (int tirage = 0; tirage < manquants && !candidats.isEmpty(); tirage++) {
            Etudiant relecteur = moinsCharge(candidats, sessionId);
            candidats.remove(relecteur);
            relectures.save(new Relecture(exercice, relecteur, maintenant));
            if (exercice.getStatut() == StatutExercice.DEPOSE) {
                exercice.passerEnAttenteDeRelecture();
            }
        }
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
