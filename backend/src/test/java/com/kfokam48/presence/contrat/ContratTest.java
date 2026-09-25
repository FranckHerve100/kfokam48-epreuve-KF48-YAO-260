package com.kfokam48.presence.contrat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ContratTest {

    @Test
    void B2_contrat_contientLesCinqOperationsImposees() {
        assertThat(Contrat.operations()).contains(
                "POST /api/sessions",
                "POST /api/presences",
                "POST /api/exercices",
                "POST /api/relectures/{id}",
                "GET /api/tableau");
    }

    @Test
    void B2_contrat_decritTreizeOperations() {
        assertThat(Contrat.operations()).hasSize(13);
    }

    @Test
    void B2_validateurAtlassian_chargeLeContrat() {
        assertThat(Contrat.conforme()).isNotNull();
    }
}
