package com.kfokam48.presence.contrat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.presence.domain.Etudiant;
import com.kfokam48.presence.domain.Promotion;
import com.kfokam48.presence.domain.Session;
import com.kfokam48.test.JeuDEssai;

/**
 * Les 5 opérations imposées, ligne par ligne du contrat : statut, $.code et conformité Atlassian.
 * On ajoute des tests à cette classe ; on n'en retire jamais (non-régression).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(JeuDEssai.class)
class ContratImposeIT {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Autowired
    private JeuDEssai jeu;

    // ───────────────────────── POST /api/sessions ─────────────────────────

    @Test
    void EF1_promotionExistante_renvoie201AvecCodeEtExpiration() throws Exception {
        Promotion promotion = jeu.promotion();

        JsonNode session = lire(ouvrirSession("{\"titre\":\"Cours 1\",\"promotionId\":" + promotion.getId() + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("[A-HJ-NP-Z2-9]{6}")))
                .andExpect(Contrat.conforme()));

        Instant ouverture = Instant.parse(session.get("ouvertureAt").asText());
        Instant expiration = Instant.parse(session.get("expirationAt").asText());
        assertThat(Duration.between(ouverture, expiration)).isEqualTo(Duration.ofMinutes(15));
    }

    @Test
    void H5_deuxSessionsOuvertesEnMemeTemps_ontDesCodesDifferents() throws Exception {
        Promotion promotion = jeu.promotion();
        String corps = "{\"titre\":\"Cours\",\"promotionId\":" + promotion.getId() + "}";

        String premier = lire(ouvrirSession(corps).andExpect(status().isCreated())).get("code").asText();
        String second = lire(ouvrirSession(corps).andExpect(status().isCreated())).get("code").asText();

        assertThat(premier).isNotEqualTo(second);
    }

    @Test
    void RG17_titreAbsent_renvoie400ChampManquant() throws Exception {
        Promotion promotion = jeu.promotion();

        ouvrirSession("{\"promotionId\":" + promotion.getId() + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"))
                .andExpect(Contrat.reponseConforme());
    }

    @Test
    void RG17_promotionIdAbsent_renvoie400ChampManquant() throws Exception {
        ouvrirSession("{\"titre\":\"Cours 1\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"))
                .andExpect(Contrat.reponseConforme());
    }

    @Test
    void EF1_promotionInexistante_renvoie400PromotionInconnue() throws Exception {
        ouvrirSession("{\"titre\":\"Cours 1\",\"promotionId\":999999}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROMOTION_INCONNUE"))
                .andExpect(Contrat.conforme());
    }

    @Test
    void B4_sessionJsonMalforme_renvoie400RequeteInvalide() throws Exception {
        ouvrirSession("{\"titre\":")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUETE_INVALIDE"))
                .andExpect(Contrat.reponseConforme());
    }

    // ───────────────────────── POST /api/presences ─────────────────────────

    @Test
    void EF2_codeValideCinqMinutesApres_renvoie201SourceEtudiant() throws Exception {
        Promotion promotion = jeu.promotion();
        Etudiant etudiant = jeu.etudiant(promotion);
        Session session = jeu.sessionOuverteIlYA(promotion, "PR2SA1", 5);

        marquer("PR2SA1", etudiant.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.sessionId").value(session.getId()))
                .andExpect(jsonPath("$.etudiantId").value(etudiant.getId()))
                .andExpect(jsonPath("$.source").value("ETUDIANT"))
                .andExpect(Contrat.conforme());
    }

    @Test
    void RG1_codeExpireApres15Minutes_renvoie410() throws Exception {
        Promotion promotion = jeu.promotion();
        Etudiant etudiant = jeu.etudiant(promotion);
        jeu.sessionOuverteIlYA(promotion, "PR2SA2", 16);

        marquer("PR2SA2", etudiant.getId())
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("CODE_EXPIRE"))
                .andExpect(Contrat.conforme());
    }

    @Test
    void RG2_dejaPresent_renvoie409() throws Exception {
        Promotion promotion = jeu.promotion();
        Etudiant etudiant = jeu.etudiant(promotion);
        Session session = jeu.sessionOuverteIlYA(promotion, "PR2SA3", 5);
        jeu.presence(session, etudiant);

        marquer("PR2SA3", etudiant.getId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEJA_PRESENT"))
                .andExpect(Contrat.conforme());
    }

    @Test
    void RG3_codeInconnu_renvoie400() throws Exception {
        Etudiant etudiant = jeu.etudiant(jeu.promotion());

        marquer("Z9Z9Z9", etudiant.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CODE_INCONNU"))
                .andExpect(Contrat.conforme());
    }

    @Test
    void RG18_etudiantAutrePromotion_renvoie400HorsPromotion() throws Exception {
        Promotion promotion = jeu.promotion();
        Etudiant autre = jeu.etudiant(jeu.promotion());
        jeu.sessionOuverteIlYA(promotion, "PR2SA4", 5);

        marquer("PR2SA4", autre.getId())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("HORS_PROMOTION"))
                .andExpect(Contrat.conforme());
    }

    @Test
    void EF2_etudiantInconnu_renvoie400EtudiantInconnu() throws Exception {
        jeu.sessionOuverteIlYA(jeu.promotion(), "PR2SA5", 5);

        marquer("PR2SA5", 999999L)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ETUDIANT_INCONNU"))
                .andExpect(Contrat.conforme());
    }

    @Test
    void RG17_presenceSansCode_renvoie400ChampManquant() throws Exception {
        mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON).content("{\"etudiantId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CHAMP_MANQUANT"))
                .andExpect(Contrat.reponseConforme());
    }

    private ResultActions marquer(String code, Long etudiantId) throws Exception {
        return mvc.perform(post("/api/presences").contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"" + code + "\",\"etudiantId\":" + etudiantId + "}"));
    }

    private ResultActions ouvrirSession(String corps) throws Exception {
        return mvc.perform(post("/api/sessions").contentType(MediaType.APPLICATION_JSON).content(corps));
    }

    private JsonNode lire(ResultActions resultat) throws Exception {
        return json.readTree(resultat.andReturn().getResponse().getContentAsString());
    }
}
