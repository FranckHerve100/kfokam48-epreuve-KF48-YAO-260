package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

public class AutoRelectureException extends ExceptionMetier {

    public AutoRelectureException() {
        super(HttpStatus.FORBIDDEN, "AUTO_RELECTURE", "Un étudiant ne peut pas relire son propre exercice.");
    }
}
