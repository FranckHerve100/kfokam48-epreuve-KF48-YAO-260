package com.kfokam48.presence.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.kfokam48.presence.contrat.Contrat;
import com.kfokam48.presence.domain.Promotion;
import com.kfokam48.test.JeuDEssai;

/** Opérations ajoutées du référentiel (cahier des charges §8), conformes au contrat. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(JeuDEssai.class)
class ReferentielIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JeuDEssai jeu;

    @Test
    void promotions_renvoie200AvecIdEtNom() throws Exception {
        Promotion promotion = jeu.promotion();

        mvc.perform(get("/api/promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nom", hasItem(promotion.getNom())))
                .andExpect(Contrat.conforme());
    }

    @Test
    void sessionsDUnePromotion_renvoie200AvecCloturePossiblementNulle() throws Exception {
        Promotion promotion = jeu.promotion();
        jeu.session(promotion, "AB23CD", Instant.parse("2026-09-25T10:00:00Z"));

        mvc.perform(get("/api/promotions/{id}/sessions", promotion.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("AB23CD"))
                .andExpect(jsonPath("$[0].expirationAt").value("2026-09-25T10:15:00Z"))
                .andExpect(jsonPath("$[0].clotureAt").isEmpty())
                .andExpect(Contrat.conforme());
    }

    @Test
    void sessionsDUnePromotionInconnue_renvoie404PromotionInconnue() throws Exception {
        mvc.perform(get("/api/promotions/{id}/sessions", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(Contrat.conforme());
    }
}
