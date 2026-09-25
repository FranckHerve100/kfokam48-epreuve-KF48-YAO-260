package com.kfokam48.presence.exception;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ErrorReportValve;

/**
 * Remplace la page HTML de Tomcat pour les erreurs levées avant la servlet (URL illisible,
 * en-têtes invalides…) par le format { code, message } du contrat (B4). Aucun détail technique.
 */
public class ErreurJsonValve extends ErrorReportValve {

    @Override
    protected void report(Request request, Response response, Throwable erreur) {
        int statut = response.getStatus();
        if (statut < 400 || response.getContentWritten() > 0 || !response.setErrorReported()) {
            return;
        }
        String corps = statut >= 500
                ? "{\"code\":\"ERREUR_INTERNE\",\"message\":\"Une erreur interne est survenue. Réessayez plus tard.\"}"
                : "{\"code\":\"REQUETE_INVALIDE\",\"message\":\"La requête est invalide.\"}";
        try {
            response.setContentType("application/json");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            Writer sortie = response.getReporter();
            if (sortie != null) {
                sortie.write(corps);
                response.finishResponse();
            }
        } catch (IOException | IllegalStateException ignoree) {
            // la réponse est déjà partie : rien d'autre à faire
        }
    }
}
