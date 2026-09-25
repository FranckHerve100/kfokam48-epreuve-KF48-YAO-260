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

import com.kfokam48.presence.domain.SourcePresence;
import com.kfokam48.presence.dto.PresenceDto;
import com.kfokam48.presence.exception.CodeExpireException;
import com.kfokam48.presence.exception.DejaPresentException;
import com.kfokam48.presence.service.PresenceService;

/** Couche web seule (service simulé) du pointage (EF2). */
@WebMvcTest(PresenceController.class)
class PresenceControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PresenceService presenceService;

    @Test
    void EF2_pointage_renvoie201() throws Exception {
        when(presenceService.marquer(any())).thenReturn(new PresenceDto(1L, 2L, 3L, SourcePresence.ETUDIANT));

        pointer("{\"code\":\"AB23CD\",\"etudiantId\":3}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.source").value("ETUDIANT"));
    }

    @Test
    void RG17_etudiantIdAbsent_renvoie400SansAppelerLeService() throws Exception {
        pointer("{\"code\":\"AB23CD\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
        verify(presenceService, never()).marquer(any());
    }

    @Test
    void RG1_codeExpire_renvoie410() throws Exception {
        when(presenceService.marquer(any())).thenThrow(new CodeExpireException());

        pointer("{\"code\":\"AB23CD\",\"etudiantId\":3}")
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"));
    }

    @Test
    void RG2_dejaPresent_renvoie409() throws Exception {
        when(presenceService.marquer(any())).thenThrow(new DejaPresentException());

        pointer("{\"code\":\"AB23CD\",\"etudiantId\":3}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"));
    }

    private org.springframework.test.web.servlet.ResultActions pointer(String corps) throws Exception {
        return mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON).content(corps));
    }
}
