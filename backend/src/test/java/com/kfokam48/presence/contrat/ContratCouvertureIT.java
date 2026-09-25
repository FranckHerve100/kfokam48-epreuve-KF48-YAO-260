package com.kfokam48.presence.contrat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Chaque opération livrée existe à la fois dans le contrat (/contrat.yaml) et dans
 * l'implémentation (/v3/api-docs). La liste grandit à chaque ticket ; on n'en retire jamais rien.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ContratCouvertureIT {

    /** Opérations livrées, au format « VERBE /chemin » du contrat. */
    static final List<String> OPERATIONS_LIVREES = List.of(
            "POST /api/sessions",
            "GET /api/promotions",
            "GET /api/promotions/{id}/sessions",
            "POST /api/presences");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void B2_contratImpose_estServiSurContratYaml() throws Exception {
        mvc.perform(get("/contrat.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("openapi: 3.0.3")));
    }

    @Test
    void B2_implementation_estDecriteSurApiDocs() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());
    }

    @Test
    void B2_swaggerUi_proposeContratImposeEtImplementation() throws Exception {
        mvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urls[*].name").value(
                        org.hamcrest.Matchers.containsInAnyOrder("Contrat imposé", "Implémentation")))
                .andExpect(jsonPath("$.urls[*].url").value(
                        org.hamcrest.Matchers.containsInAnyOrder("/contrat.yaml", "/v3/api-docs")));
    }

    @Test
    void B2_operationsLivrees_existentDansLeContratEtDansLImplementation() throws Exception {
        Set<String> contrat = Contrat.operations();
        Set<String> implementation = operationsImplementees();

        assertThat(contrat).containsAll(OPERATIONS_LIVREES);
        assertThat(implementation).containsAll(OPERATIONS_LIVREES);
    }

    private Set<String> operationsImplementees() throws Exception {
        String docs = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        JsonNode chemins = json.readTree(docs).path("paths");
        Set<String> operations = new HashSet<>();
        for (Iterator<Map.Entry<String, JsonNode>> it = chemins.fields(); it.hasNext();) {
            Map.Entry<String, JsonNode> chemin = it.next();
            chemin.getValue().fieldNames()
                    .forEachRemaining(verbe -> operations.add(verbe.toUpperCase() + " " + chemin.getKey()));
        }
        return operations;
    }
}
