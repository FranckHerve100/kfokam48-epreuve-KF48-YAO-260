package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

/** 400 quand l'identifiant vient du corps de la requête, 404 quand il vient du chemin. */
public class PromotionInconnueException extends ExceptionMetier {

    private PromotionInconnueException(HttpStatus statut) {
        super(statut, "PROMOTION_INCONNUE", "Cette promotion n'existe pas.");
    }

    public static PromotionInconnueException enCorps() {
        return new PromotionInconnueException(HttpStatus.BAD_REQUEST);
    }

    public static PromotionInconnueException enChemin() {
        return new PromotionInconnueException(HttpStatus.NOT_FOUND);
    }
}
