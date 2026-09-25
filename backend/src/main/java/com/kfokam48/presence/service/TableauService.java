package com.kfokam48.presence.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.presence.dto.LigneTableauDto;
import com.kfokam48.presence.exception.PromotionInconnueException;
import com.kfokam48.presence.repository.EtudiantRepository;
import com.kfokam48.presence.repository.LigneTableau;
import com.kfokam48.presence.repository.PromotionRepository;

/** Tableau de suivi d'une promotion (EF6), calculé en une requête agrégée (ENF2). */
@Service
@Transactional(readOnly = true)
public class TableauService {

    private final PromotionRepository promotions;
    private final EtudiantRepository etudiants;

    public TableauService(PromotionRepository promotions, EtudiantRepository etudiants) {
        this.promotions = promotions;
        this.etudiants = etudiants;
    }

    /**
     * Le contrat n'autorise que 200 et 404 sur cette opération imposée : un identifiant absent,
     * non numérique ou inconnu renvoie donc 404 PROMOTION_INCONNUE.
     */
    public List<LigneTableauDto> tableau(String promotionIdBrut) {
        Long promotionId = identifiant(promotionIdBrut);
        if (promotionId == null || !promotions.existsById(promotionId)) {
            throw PromotionInconnueException.enChemin();
        }
        return etudiants.tableau(promotionId).stream().map(TableauService::versDto).toList();
    }

    private static Long identifiant(String brut) {
        if (brut == null || brut.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(brut.strip());
        } catch (NumberFormatException nonNumerique) {
            return null;
        }
    }

    private static LigneTableauDto versDto(LigneTableau ligne) {
        BigDecimal moyenne = ligne.moyenne() == null
                ? null
                : BigDecimal.valueOf(ligne.moyenne()).setScale(2, RoundingMode.HALF_UP);
        return new LigneTableauDto(ligne.etudiantId(), ligne.nom(), ligne.presences().intValue(),
                ligne.exercicesDeposes().intValue(), moyenne, ligne.relecturesEnAttente().intValue());
    }
}
