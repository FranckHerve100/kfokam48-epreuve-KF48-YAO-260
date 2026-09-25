package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

public class DejaPresentException extends ExceptionMetier {

    public DejaPresentException() {
        super(HttpStatus.CONFLICT, "DEJA_PRESENT", "La présence est déjà enregistrée pour cette session.");
    }
}
