package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

public class CodeExpireException extends ExceptionMetier {

    public CodeExpireException() {
        super(HttpStatus.GONE, "CODE_EXPIRE", "Le code de présence a expiré.");
    }
}
