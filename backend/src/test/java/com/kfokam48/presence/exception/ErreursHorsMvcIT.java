package com.kfokam48.presence.exception;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.RequestDispatcher;

/**
 * Les erreurs levées avant Spring MVC (conteneur de servlets) passent par /error :
 * elles gardent le format { code, message } du contrat (B4, ENF4).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErreursHorsMvcIT {

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource({"400, REQUETE_INVALIDE", "404, RESSOURCE_INCONNUE", "500, ERREUR_INTERNE"})
    void B4_erreurDuConteneur_gardeLeFormatDuContrat(int statut, String code) throws Exception {
        mvc.perform(get("/error")
                        .requestAttr(RequestDispatcher.ERROR_STATUS_CODE, statut)
                        .requestAttr(RequestDispatcher.ERROR_REQUEST_URI, "/api/%")
                        .requestAttr(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("détail interne")))
                .andExpect(status().is(statut))
                .andExpect(jsonPath("$", aMapWithSize(2)))
                .andExpect(jsonPath("$.code").value(code))
                .andExpect(jsonPath("$.message").isString());
    }
}
