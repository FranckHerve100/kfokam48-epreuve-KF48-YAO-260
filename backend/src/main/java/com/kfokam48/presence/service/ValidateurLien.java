package com.kfokam48.presence.service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/** RG16 : le lien d'un exercice est une URL http ou https absolue, avec un hôte, de 2000 caractères au plus. */
final class ValidateurLien {

    static final int LONGUEUR_MAX = 2000;
    private static final Set<String> SCHEMAS = Set.of("http", "https");

    private ValidateurLien() {
    }

    static boolean estValide(String lien) {
        if (lien == null || lien.length() > LONGUEUR_MAX) {
            return false;
        }
        try {
            URI uri = new URI(lien.strip());
            return uri.getScheme() != null
                    && SCHEMAS.contains(uri.getScheme().toLowerCase(Locale.ROOT))
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (URISyntaxException illisible) {
            return false;
        }
    }
}
