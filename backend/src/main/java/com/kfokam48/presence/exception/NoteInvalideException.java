package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

public class NoteInvalideException extends ExceptionMetier {

    public NoteInvalideException() {
        super(HttpStatus.BAD_REQUEST, "NOTE_INVALIDE", "La note doit être un entier compris entre 0 et 20.");
    }
}
