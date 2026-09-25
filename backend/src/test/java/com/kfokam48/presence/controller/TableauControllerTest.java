package com.kfokam48.presence.controller;

import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.kfokam48.presence.dto.LigneTableauDto;
import com.kfokam48.presence.exception.PromotionInconnueException;
import com.kfokam48.presence.service.TableauService;

/** Couche web seule (service simulé) du tableau (EF6). */
@WebMvcTest(TableauController.class)
class TableauControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private TableauService tableauService;

    @Test
    void EF6_tableau_renvoie200AvecMoyenneADeuxDecimalesEtNulle() throws Exception {
        when(tableauService.tableau("1")).thenReturn(List.of(
                new LigneTableauDto(10L, "Abena", 3, 2, new BigDecimal("14.00"), 0),
                new LigneTableauDto(11L, "Boris", 1, 0, null, 1)));

        mvc.perform(get("/api/tableau").param("promotionId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].moyenne").value(14.00))
                .andExpect(jsonPath("$[1].moyenne").isEmpty())
                .andExpect(jsonPath("$[1].relecturesEnAttente").value(1));
    }

    @Test
    void EF6_parametreAbsent_renvoie404PromotionInconnue() throws Exception {
        when(tableauService.tableau(isNull())).thenThrow(PromotionInconnueException.enChemin());

        mvc.perform(get("/api/tableau"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }

    @Test
    void EF6_parametreNonNumerique_renvoie404PromotionInconnue() throws Exception {
        when(tableauService.tableau("abc")).thenThrow(PromotionInconnueException.enChemin());

        mvc.perform(get("/api/tableau").param("promotionId", "abc"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"));
    }
}
