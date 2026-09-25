package com.kfokam48.test;

import java.time.Instant;

import org.springframework.boot.test.context.TestComponent;

import com.kfokam48.presence.domain.Etudiant;
import com.kfokam48.presence.domain.Exercice;
import com.kfokam48.presence.domain.Presence;
import com.kfokam48.presence.domain.Promotion;
import com.kfokam48.presence.domain.Session;
import com.kfokam48.presence.domain.SourcePresence;
import com.kfokam48.presence.repository.EtudiantRepository;
import com.kfokam48.presence.repository.ExerciceRepository;
import com.kfokam48.presence.repository.PresenceRepository;
import com.kfokam48.presence.repository.PromotionRepository;
import com.kfokam48.presence.repository.SessionRepository;

/** Création de données de test en base H2 (importé explicitement par les tests d'intégration). */
@TestComponent
public class JeuDEssai {

    private static int compteur;

    private final PromotionRepository promotions;
    private final EtudiantRepository etudiants;
    private final SessionRepository sessions;
    private final PresenceRepository presences;
    private final ExerciceRepository exercices;

    public JeuDEssai(PromotionRepository promotions, EtudiantRepository etudiants, SessionRepository sessions,
                     PresenceRepository presences, ExerciceRepository exercices) {
        this.promotions = promotions;
        this.etudiants = etudiants;
        this.sessions = sessions;
        this.presences = presences;
        this.exercices = exercices;
    }

    public Promotion promotion() {
        return promotions.save(new Promotion("Promotion " + (++compteur)));
    }

    public Etudiant etudiant(Promotion promotion) {
        return etudiants.save(new Etudiant("Étudiant " + (++compteur), promotion));
    }

    public Session session(Promotion promotion, String code, Instant ouverture) {
        return sessions.save(new Session("Session " + (++compteur), promotion, code, ouverture));
    }

    /** Session ouverte il y a {@code minutes} minutes (horloge réelle). */
    public Session sessionOuverteIlYA(Promotion promotion, String code, long minutes) {
        return session(promotion, code, Instant.now().minusSeconds(minutes * 60));
    }

    public Presence presence(Session session, Etudiant etudiant) {
        return presences.save(new Presence(session, etudiant, SourcePresence.ETUDIANT, Instant.now()));
    }

    public Exercice exercice(Session session, Etudiant auteur) {
        return exercices.save(new Exercice(session, auteur, "https://github.com/essai/" + (++compteur), Instant.now()));
    }
}
