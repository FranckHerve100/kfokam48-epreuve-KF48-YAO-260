package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

/** 400 quand l'identifiant vient du corps de la requête, 404 quand il vient du chemin. */
public class SessionInconnueException extends ExceptionMetier {

    private SessionInconnueException(HttpStatus statut) {
        super(statut, "SESSION_INCONNUE", "Cette session n'existe pas.");
    }

    public static SessionInconnueException enCorps() {
        return new SessionInconnueException(HttpStatus.BAD_REQUEST);
    }

    public static SessionInconnueException enChemin() {
        return new SessionInconnueException(HttpStatus.NOT_FOUND);
    }
}
