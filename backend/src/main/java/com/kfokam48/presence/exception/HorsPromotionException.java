package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

public class HorsPromotionException extends ExceptionMetier {

    public HorsPromotionException() {
        super(HttpStatus.BAD_REQUEST, "HORS_PROMOTION", "Cet étudiant n'appartient pas à la promotion de la session.");
    }
}
