package com.kfokam48.presence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * Le contexte démarre sur H2 : Flyway applique V1 et V2 puis Hibernate valide le mapping
 * (ddl-auto=validate). Toute divergence entre entités et schéma fait échouer ce test (B5).
 */
@SpringBootTest
@ActiveProfiles("test")
class PresenceApplicationIT {

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbc;

    /** Version courante du schéma : V2 depuis le passage à deux relecteurs (#52). */
    @Test
    void B5_contexteDemarre_schemaV2AppliqueEtValide() {
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("2");
        assertThat(flyway.info().applied()).hasSize(2);
    }

    @Test
    void B5_schemaV1_contientLesSixTablesDeD2() {
        List<String> tables = jdbc.queryForList(
                "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'", String.class);

        assertThat(tables).contains("promotion", "etudiant", "session", "presence", "exercice", "relecture");
    }

    @Test
    void B6_profilTest_utiliseH2SansBaseLocale() throws Exception {
        String produit = jdbc.getDataSource().getConnection().getMetaData().getDatabaseProductName();

        assertThat(produit).isEqualTo("H2");
    }
}
