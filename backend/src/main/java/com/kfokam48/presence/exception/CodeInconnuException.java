package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

public class CodeInconnuException extends ExceptionMetier {

    public CodeInconnuException() {
        super(HttpStatus.BAD_REQUEST, "CODE_INCONNU", "Ce code ne correspond à aucune session ouverte.");
    }
}
