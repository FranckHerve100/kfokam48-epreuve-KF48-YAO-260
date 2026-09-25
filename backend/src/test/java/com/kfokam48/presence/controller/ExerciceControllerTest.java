package com.kfokam48.presence.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.kfokam48.presence.domain.StatutExercice;
import com.kfokam48.presence.dto.ExerciceDeposeDto;
import com.kfokam48.presence.exception.ExerciceDejaDeposeException;
import com.kfokam48.presence.exception.LienInvalideException;
import com.kfokam48.presence.exception.SessionClotureeException;
import com.kfokam48.presence.service.ExerciceService;

/** Couche web seule (service simulé) du dépôt d'exercice (EF3). */
@WebMvcTest(ExerciceController.class)
class ExerciceControllerTest {

    private static final String CORPS = "{\"sessionId\":1,\"etudiantId\":2,\"lien\":\"https://github.com/a/b\"}";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ExerciceService exerciceService;

    @Test
    void EF3_depot_renvoie201AvecIdEtStatut() throws Exception {
        when(exerciceService.deposer(any())).thenReturn(new ExerciceDeposeDto(5L, StatutExercice.DEPOSE));

        deposer(CORPS).andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.statut").value("DEPOSE"));
    }

    @Test
    void RG17_lienAbsent_renvoie400SansAppelerLeService() throws Exception {
        deposer("{\"sessionId\":1,\"etudiantId\":2}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
        verify(exerciceService, never()).deposer(any());
    }

    @Test
    void RG16_lienInvalide_renvoie400() throws Exception {
        when(exerciceService.deposer(any())).thenThrow(new LienInvalideException());

        deposer(CORPS).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("LIEN_INVALIDE"));
    }

    @Test
    void RG6_secondDepot_renvoie409() throws Exception {
        when(exerciceService.deposer(any())).thenThrow(new ExerciceDejaDeposeException());

        deposer(CORPS).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("EXERCICE_DEJA_DEPOSE"));
    }

    @Test
    void RG7_sessionCloturee_renvoie409() throws Exception {
        when(exerciceService.deposer(any())).thenThrow(new SessionClotureeException());

        deposer(CORPS).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("SESSION_CLOTUREE"));
    }

    private ResultActions deposer(String corps) throws Exception {
        return mvc.perform(post("/api/exercices").contentType(MediaType.APPLICATION_JSON).content(corps));
    }
}
