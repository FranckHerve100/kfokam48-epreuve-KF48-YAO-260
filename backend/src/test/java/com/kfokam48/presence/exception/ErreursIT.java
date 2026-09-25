package com.kfokam48.presence.exception;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.kfokam48.test.ControleurEssai;

/** Toute erreur renvoie { code, message } et rien d'autre, jamais de stack trace (B4, RG17, ENF4). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ControleurEssai.class)
class ErreursIT {

    @Autowired
    private MockMvc mvc;

    @Test
    void RG17_champObligatoireAbsent_renvoie400ChampManquant() throws Exception {
        formatErreur(mvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Cours 1\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    void RG17_champObligatoireVide_renvoie400ChampManquant() throws Exception {
        formatErreur(mvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"  \",\"promotionId\":1}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"));
    }

    @Test
    void B4_jsonMalforme_renvoie400RequeteInvalide() throws Exception {
        formatErreur(mvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\": ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
    }

    @Test
    void B4_champMalType_renvoie400RequeteInvalide() throws Exception {
        formatErreur(mvc.perform(post("/test/validation").contentType(MediaType.APPLICATION_JSON)
                .content("{\"titre\":\"Cours 1\",\"promotionId\":\"abc\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
    }

    @Test
    void B4_parametreManquantOuMalType_renvoie400RequeteInvalide() throws Exception {
        formatErreur(mvc.perform(get("/test/parametre")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
        formatErreur(mvc.perform(get("/test/parametre").param("valeur", "abc")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
    }

    @Test
    void B4_routeInconnue_renvoie404RessourceInconnue() throws Exception {
        formatErreur(mvc.perform(get("/api/inexistant")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESSOURCE_INCONNUE"));
    }

    @Test
    void B4_verbeNonSupporte_renvoie405AuFormatDuContrat() throws Exception {
        formatErreur(mvc.perform(delete("/test/validation")))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"));
    }

    @Test
    void B4_exceptionMetier_renvoieSonStatutEtSonCode() throws Exception {
        formatErreur(mvc.perform(get("/test/metier")))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"));
    }

    @Test
    void B4_exceptionInattendue_renvoie500ErreurInterneSansDetail() throws Exception {
        formatErreur(mvc.perform(get("/test/panne")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("ERREUR_INTERNE"))
                .andExpect(content().string(not(containsString("détail interne"))))
                .andExpect(content().string(not(containsString("IllegalStateException"))));
    }

    /** Corps JSON à exactement deux champs : aucun trace, exception, path, error ni timestamp de Spring. */
    private static ResultActions formatErreur(ResultActions resultat) throws Exception {
        return resultat
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", aMapWithSize(2)))
                .andExpect(jsonPath("$.code").isString())
                .andExpect(jsonPath("$.message").isString());
    }
}
