package com.kfokam48.presence.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kfokam48.presence.dto.SessionOuverteDto;
import com.kfokam48.presence.exception.PromotionInconnueException;
import com.kfokam48.presence.service.SessionService;

/** Couche web seule (service simulé) : statuts, validation et traduction des erreurs (EF1, RG17). */
@WebMvcTest(SessionController.class)
class SessionControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SessionService sessionService;

    @Test
    void EF1_ouverture_renvoie201EtLaSession() throws Exception {
        when(sessionService.ouvrir(any())).thenReturn(new SessionOuverteDto(7L, "AB23CD",
                Instant.parse("2026-09-25T10:00:00Z"), Instant.parse("2026-09-25T10:15:00Z")));

        mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Cours 1\",\"promotionId\":1}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.code").value("AB23CD"))
                .andExpect(jsonPath("$.expirationAt").value("2026-09-25T10:15:00Z"));
    }

    @Test
    void RG17_titreVide_renvoie400ChampManquantSansAppelerLeService() throws Exception {
        mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"\",\"promotionId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
        verify(sessionService, never()).ouvrir(any());
    }

    @Test
    void EF1_promotionInconnue_renvoie400PromotionInconnue() throws Exception {
        when(sessionService.ouvrir(any())).thenThrow(PromotionInconnueException.enCorps());

        mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Cours 1\",\"promotionId\":99}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }
}
