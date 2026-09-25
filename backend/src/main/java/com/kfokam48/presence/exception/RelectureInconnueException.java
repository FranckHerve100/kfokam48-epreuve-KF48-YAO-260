package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

public class RelectureInconnueException extends ExceptionMetier {

    public RelectureInconnueException() {
        super(HttpStatus.BAD_REQUEST, "RELECTURE_INCONNUE", "Cette relecture n'existe pas.");
    }
}
