package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

/**
 * Erreur métier traduite par {@link GlobalExceptionHandler} en { code, message }.
 * Le statut et le code suivent le catalogue en tête de api/contrat.yaml.
 */
public abstract class ExceptionMetier extends RuntimeException {

    private final HttpStatus statut;
    private final String code;

    protected ExceptionMetier(HttpStatus statut, String code, String message) {
        super(message);
        this.statut = statut;
        this.code = code;
    }

    public HttpStatus getStatut() {
        return statut;
    }

    public String getCode() {
        return code;
    }
}
