package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

public class RelecteurNonAssigneException extends ExceptionMetier {

    public RelecteurNonAssigneException() {
        super(HttpStatus.FORBIDDEN, "RELECTEUR_NON_ASSIGNE", "Cette relecture est assignée à un autre étudiant.");
    }
}
