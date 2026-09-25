package com.kfokam48.presence.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * Une URL illisible est rejetée par Tomcat avant la servlet : la réponse garde tout de même
 * le format { code, message }, jamais la page HTML de Tomcat (B4, ENF4).
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ErreursTomcatIT {

    @LocalServerPort
    private int port;

    @Test
    void B4_urlMalEncodee_renvoie400JsonAuFormatDuContrat() throws Exception {
        String reponse = requeteBrute("GET /api/%zz HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n");

        assertThat(reponse)
                .startsWith("HTTP/1.1 400")
                .containsIgnoringCase("Content-Type: application/json")
                .contains("\"code\":\"REQUETE_INVALIDE\"")
                .doesNotContain("<html")
                .doesNotContain("Tomcat");
    }

    private String requeteBrute(String requete) throws Exception {
        try (Socket socket = new Socket("localhost", port)) {
            OutputStream sortie = socket.getOutputStream();
            sortie.write(requete.getBytes(StandardCharsets.US_ASCII));
            sortie.flush();
            InputStream entree = socket.getInputStream();
            return new String(entree.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
