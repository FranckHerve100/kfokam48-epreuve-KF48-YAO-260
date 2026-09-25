package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

public class RelectureDejaRendueException extends ExceptionMetier {

    public RelectureDejaRendueException() {
        super(HttpStatus.CONFLICT, "RELECTURE_DEJA_RENDUE", "Cette relecture a déjà été rendue : la note est définitive.");
    }
}
