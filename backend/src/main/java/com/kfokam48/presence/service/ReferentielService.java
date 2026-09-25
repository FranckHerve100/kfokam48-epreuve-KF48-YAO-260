package com.kfokam48.presence.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.presence.domain.Session;
import com.kfokam48.presence.dto.ReferenceDto;
import com.kfokam48.presence.dto.SessionDto;
import com.kfokam48.presence.exception.PromotionInconnueException;
import com.kfokam48.presence.repository.PromotionRepository;
import com.kfokam48.presence.repository.SessionRepository;

/** Listes de référence des écrans : promotions, sessions et étudiants d'une promotion (Q1, H7). */
@Service
@Transactional(readOnly = true)
public class ReferentielService {

    private final PromotionRepository promotions;
    private final SessionRepository sessions;

    public ReferentielService(PromotionRepository promotions, SessionRepository sessions) {
        this.promotions = promotions;
        this.sessions = sessions;
    }

    public List<ReferenceDto> promotions() {
        return promotions.findAllByOrderByNomAsc().stream()
                .map(promotion -> new ReferenceDto(promotion.getId(), promotion.getNom()))
                .toList();
    }

    /** Sessions d'une promotion, la plus récente en premier ; 404 si la promotion n'existe pas. */
    public List<SessionDto> sessionsDeLaPromotion(Long promotionId) {
        verifierPromotion(promotionId);
        return sessions.findByPromotionIdOrderByOuvertureAtDesc(promotionId).stream()
                .map(ReferentielService::versDto)
                .toList();
    }

    private void verifierPromotion(Long promotionId) {
        if (!promotions.existsById(promotionId)) {
            throw PromotionInconnueException.enChemin();
        }
    }

    private static SessionDto versDto(Session session) {
        return new SessionDto(session.getId(), session.getTitre(), session.getCode(), session.getOuvertureAt(),
                session.getExpirationAt(), session.getClotureAt());
    }
}
