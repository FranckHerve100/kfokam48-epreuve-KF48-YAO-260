package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

public class LienInvalideException extends ExceptionMetier {

    public LienInvalideException() {
        super(HttpStatus.BAD_REQUEST, "LIEN_INVALIDE", "Le lien doit être une URL http ou https valide.");
    }
}
