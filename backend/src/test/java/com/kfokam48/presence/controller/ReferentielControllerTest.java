package com.kfokam48.presence.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kfokam48.presence.dto.ReferenceDto;
import com.kfokam48.presence.dto.SessionDto;
import com.kfokam48.presence.exception.PromotionInconnueException;
import com.kfokam48.presence.service.ReferentielService;

/** Couche web seule (service simulé) du référentiel. */
@WebMvcTest(ReferentielController.class)
class ReferentielControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReferentielService referentielService;

    @Test
    void promotions_renvoie200() throws Exception {
        when(referentielService.promotions()).thenReturn(List.of(new ReferenceDto(1L, "KF48 Yaoundé")));

        mvc.perform(get("/api/promotions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("KF48 Yaoundé"));
    }

    @Test
    void sessions_renvoie200AvecClotureNulle() throws Exception {
        when(referentielService.sessionsDeLaPromotion(1L)).thenReturn(List.of(new SessionDto(3L, "Cours", "AB23CD",
                Instant.parse("2026-09-25T10:00:00Z"), Instant.parse("2026-09-25T10:15:00Z"), null)));

        mvc.perform(get("/api/promotions/1/sessions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("AB23CD"))
                .andExpect(jsonPath("$[0].clotureAt").isEmpty());
    }

    @Test
    void sessions_promotionInconnue_renvoie404() throws Exception {
        when(referentielService.sessionsDeLaPromotion(99L)).thenThrow(PromotionInconnueException.enChemin());

        mvc.perform(get("/api/promotions/99/sessions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }

    @Test
    void sessions_identifiantNonNumerique_renvoie400RequeteInvalide() throws Exception {
        mvc.perform(get("/api/promotions/abc/sessions"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
    }

    @Test
    void etudiants_renvoie200() throws Exception {
        when(referentielService.etudiantsDeLaPromotion(1L)).thenReturn(List.of(new ReferenceDto(4L, "Abena Mvondo")));

        mvc.perform(get("/api/promotions/1/etudiants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nom").value("Abena Mvondo"));
    }
}
