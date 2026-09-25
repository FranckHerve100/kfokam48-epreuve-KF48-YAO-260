package com.kfokam48.presence.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.kfokam48.presence.dto.RelectureAssigneeDto;
import com.kfokam48.presence.dto.StatutRelecture;
import com.kfokam48.presence.exception.AutoRelectureException;
import com.kfokam48.presence.exception.RelectureDejaRendueException;
import com.kfokam48.presence.service.RelectureService;

/** Couche web seule (service simulé) de la relecture (EF5, RG11). */
@WebMvcTest({RelectureController.class, EtudiantController.class})
class RelectureControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private RelectureService relectureService;

    @Test
    void EF5_noteValideAvecEnTete_renvoie200SansCorps() throws Exception {
        rendre("{\"note\":14,\"commentaire\":\"Bien\"}", "11")
                .andExpect(status().isOk())
                .andExpect(content().string(""));
        verify(relectureService).rendre(eq(7L), any(), eq(11L));
    }

    @Test
    void H11_sansEnTete_renvoie200() throws Exception {
        rendre("{\"note\":14,\"commentaire\":\"Bien\"}", null).andExpect(status().isOk());
        verify(relectureService).rendre(eq(7L), any(), isNull());
    }

    @ParameterizedTest
    @ValueSource(strings = {"21", "-1", "12.5", "\"quinze\""})
    void RG11_noteHorsBornesDecimaleOuTexte_renvoie400NoteInvalide(String note) throws Exception {
        rendre("{\"note\":" + note + ",\"commentaire\":\"Bien\"}", "11")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("NOTE_INVALIDE"));
        verify(relectureService, never()).rendre(any(), any(), any());
    }

    @Test
    void RG11_bornes0Et20_acceptees() throws Exception {
        rendre("{\"note\":0,\"commentaire\":\"À reprendre\"}", "11").andExpect(status().isOk());
        rendre("{\"note\":20,\"commentaire\":\"Parfait\"}", "11").andExpect(status().isOk());
    }

    @Test
    void RG17_noteAbsente_renvoie400ChampManquant() throws Exception {
        rendre("{\"commentaire\":\"Bien\"}", "11")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    void RG10_autoRelecture_renvoie403() throws Exception {
        doThrow(new AutoRelectureException()).when(relectureService).rendre(any(), any(), any());

        rendre("{\"note\":14,\"commentaire\":\"Bien\"}", "10")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTO_RELECTURE"));
    }

    @Test
    void RG12_dejaRendue_renvoie409() throws Exception {
        doThrow(new RelectureDejaRendueException()).when(relectureService).rendre(any(), any(), any());

        rendre("{\"note\":14,\"commentaire\":\"Bien\"}", "11")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELECTURE_DEJA_RENDUE"));
    }

    @Test
    void H11_enTeteNonNumerique_renvoie400RequeteInvalide() throws Exception {
        rendre("{\"note\":14,\"commentaire\":\"Bien\"}", "abc")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
    }

    @Test
    void EF5_relecturesDUnEtudiant_renvoie200() throws Exception {
        when(relectureService.relecturesDe(11L)).thenReturn(List.of(
                new RelectureAssigneeDto(7L, 5L, "https://github.com/a/b", StatutRelecture.EN_ATTENTE)));

        mvc.perform(get("/api/etudiants/11/relectures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].exerciceId").value(5))
                .andExpect(jsonPath("$[0].statut").value("EN_ATTENTE"));
    }

    private ResultActions rendre(String corps, String etudiantId) throws Exception {
        var requete = post("/api/relectures/7").contentType(MediaType.APPLICATION_JSON).content(corps);
        if (etudiantId != null) {
            requete.header("X-Etudiant-Id", etudiantId);
        }
        return mvc.perform(requete);
    }
}
