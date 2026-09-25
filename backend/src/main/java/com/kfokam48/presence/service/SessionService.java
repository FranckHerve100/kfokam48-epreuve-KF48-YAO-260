package com.kfokam48.presence.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.presence.domain.Promotion;
import com.kfokam48.presence.domain.Session;
import com.kfokam48.presence.dto.OuvertureSessionDemande;
import com.kfokam48.presence.dto.SessionOuverteDto;
import com.kfokam48.presence.exception.PromotionInconnueException;
import com.kfokam48.presence.repository.PromotionRepository;
import com.kfokam48.presence.repository.SessionRepository;

/** Ouverture des sessions et de leur code de présence (EF1, RG1, H5). */
@Service
public class SessionService {

    /** Au-delà, le tirage d'un code libre est considéré comme une anomalie (≈ 1 milliard de codes possibles). */
    static final int TIRAGES_MAX = 20;

    private final SessionRepository sessions;
    private final PromotionRepository promotions;
    private final GenerateurCode generateur;
    private final Clock horloge;

    public SessionService(SessionRepository sessions, PromotionRepository promotions,
                          GenerateurCode generateur, Clock horloge) {
        this.sessions = sessions;
        this.promotions = promotions;
        this.generateur = generateur;
        this.horloge = horloge;
    }

    /** Ouvre une session : son code expire 15 minutes plus tard (RG1) et n'est porté par aucune session en cours (H5). */
    @Transactional
    public SessionOuverteDto ouvrir(OuvertureSessionDemande demande) {
        Promotion promotion = promotions.findById(demande.promotionId())
                .orElseThrow(PromotionInconnueException::enCorps);
        Instant maintenant = Instant.now(horloge).truncatedTo(ChronoUnit.SECONDS);

        Session session = sessions.save(new Session(demande.titre().strip(), promotion, codeLibre(maintenant), maintenant));

        return new SessionOuverteDto(session.getId(), session.getCode(), session.getOuvertureAt(), session.getExpirationAt());
    }

    private String codeLibre(Instant maintenant) {
        for (int tirage = 0; tirage < TIRAGES_MAX; tirage++) {
            String code = generateur.nouveauCode();
            if (!sessions.existsByCodeAndExpirationAtGreaterThanEqual(code, maintenant)) {
                return code;
            }
        }
        throw new IllegalStateException("Aucun code de session libre après " + TIRAGES_MAX + " tirages");
    }
}
