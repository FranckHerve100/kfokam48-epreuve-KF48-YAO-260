package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

public class SessionClotureeException extends ExceptionMetier {

    public SessionClotureeException() {
        super(HttpStatus.CONFLICT, "SESSION_CLOTUREE", "La session est clôturée.");
    }
}
