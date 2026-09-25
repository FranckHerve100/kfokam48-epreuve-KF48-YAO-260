package com.kfokam48.presence.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.presence.domain.Etudiant;
import com.kfokam48.presence.domain.Presence;
import com.kfokam48.presence.domain.Session;
import com.kfokam48.presence.domain.SourcePresence;
import com.kfokam48.presence.dto.MarquagePresenceDemande;
import com.kfokam48.presence.dto.PresenceDto;
import com.kfokam48.presence.exception.CodeExpireException;
import com.kfokam48.presence.exception.CodeInconnuException;
import com.kfokam48.presence.exception.DejaPresentException;
import com.kfokam48.presence.exception.EtudiantInconnuException;
import com.kfokam48.presence.exception.HorsPromotionException;
import com.kfokam48.presence.repository.EtudiantRepository;
import com.kfokam48.presence.repository.PresenceRepository;
import com.kfokam48.presence.repository.SessionRepository;

/** Pointage par code, dans l'ordre des contrôles du diagramme D3 (EF2). */
@Service
public class PresenceService {

    private final EtudiantRepository etudiants;
    private final SessionRepository sessions;
    private final PresenceRepository presences;
    private final Clock horloge;

    public PresenceService(EtudiantRepository etudiants, SessionRepository sessions, PresenceRepository presences,
                           Clock horloge) {
        this.etudiants = etudiants;
        this.sessions = sessions;
        this.presences = presences;
        this.horloge = horloge;
    }

    /**
     * Ordre D3 : étudiant connu → code connu (RG3) → code non expiré et session non clôturée (RG1, H2)
     * → même promotion (RG18) → pas déjà présent (RG2) → présence source ETUDIANT.
     * Le blocage après 5 codes erronés (RG4) est livré à l'étape 4.
     */
    @Transactional
    public PresenceDto marquer(MarquagePresenceDemande demande) {
        Etudiant etudiant = etudiants.findById(demande.etudiantId())
                .orElseThrow(EtudiantInconnuException::enCorps);
        Session session = sessions.findFirstByCodeOrderByOuvertureAtDesc(normaliser(demande.code()))
                .orElseThrow(CodeInconnuException::new);
        Instant maintenant = Instant.now(horloge).truncatedTo(ChronoUnit.SECONDS);

        if (session.refusePointageA(maintenant)) {
            throw new CodeExpireException();
        }
        if (!etudiant.appartientA(session.getPromotion())) {
            throw new HorsPromotionException();
        }
        if (presences.existsBySessionIdAndEtudiantId(session.getId(), etudiant.getId())) {
            throw new DejaPresentException();
        }
        Presence presence = enregistrer(new Presence(session, etudiant, SourcePresence.ETUDIANT, maintenant));
        return new PresenceDto(presence.getId(), session.getId(), etudiant.getId(), presence.getSource());
    }

    /** La contrainte d'unicité (session, étudiant) tranche un double envoi simultané du même étudiant (RG2). */
    private Presence enregistrer(Presence presence) {
        try {
            return presences.saveAndFlush(presence);
        } catch (DataIntegrityViolationException doublon) {
            throw new DejaPresentException();
        }
    }

    /** Le code s'affiche en majuscules ; l'étudiant peut le saisir en minuscules, avec des espaces autour. */
    private static String normaliser(String code) {
        return code.strip().toUpperCase(Locale.ROOT);
    }
}
