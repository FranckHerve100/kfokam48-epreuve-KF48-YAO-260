package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

public class ExerciceDejaDeposeException extends ExceptionMetier {

    public ExerciceDejaDeposeException() {
        super(HttpStatus.CONFLICT, "EXERCICE_DEJA_DEPOSE", "Un exercice a déjà été déposé pour cette session.");
    }
}
