package com.kfokam48.presence.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

/**
 * Corps de /error, utilisé pour les erreurs levées avant Spring MVC (conteneur de servlets) :
 * même format { code, message } que {@link GlobalExceptionHandler}, sans aucun détail technique.
 */
@Component
public class ErreurAttributs extends DefaultErrorAttributes {

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest requete, ErrorAttributeOptions options) {
        Object statut = super.getErrorAttributes(requete, ErrorAttributeOptions.defaults()).get("status");
        int code = statut instanceof Integer valeur ? valeur : 500;

        Map<String, Object> corps = new LinkedHashMap<>();
        if (code == 404) {
            corps.put("code", "RESSOURCE_INCONNUE");
            corps.put("message", "Cette adresse n'existe pas dans l'API.");
        } else if (code >= 400 && code < 500) {
            corps.put("code", GlobalExceptionHandler.REQUETE_INVALIDE);
            corps.put("message", "La requête est invalide.");
        } else {
            corps.put("code", "ERREUR_INTERNE");
            corps.put("message", "Une erreur interne est survenue. Réessayez plus tard.");
        }
        return corps;
    }
}
