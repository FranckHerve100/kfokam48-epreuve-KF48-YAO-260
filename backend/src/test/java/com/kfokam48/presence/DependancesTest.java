package com.kfokam48.presence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootVersion;

/** Compatibilité des dépendances majeures avec Spring Boot 3.5 (ADR-2). */
class DependancesTest {

    @Test
    void NR_33_springdocResteEnVersion2PourSpringBoot3() throws Exception {
        assertThat(SpringBootVersion.getVersion()).startsWith("3.");
        assertThat(versionMaven("org.springdoc", "springdoc-openapi-starter-webmvc-ui"))
                .as("springdoc 3.x cible Spring Boot 4 et empêche l'application de démarrer (#33)")
                .startsWith("2.");
    }

    private static String versionMaven(String groupe, String artefact) throws Exception {
        String chemin = "META-INF/maven/%s/%s/pom.properties".formatted(groupe, artefact);
        try (InputStream flux = DependancesTest.class.getClassLoader().getResourceAsStream(chemin)) {
            assertThat(flux).as(chemin).isNotNull();
            Properties proprietes = new Properties();
            proprietes.load(flux);
            return proprietes.getProperty("version");
        }
    }
}
