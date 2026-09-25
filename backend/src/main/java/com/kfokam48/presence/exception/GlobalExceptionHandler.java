package com.kfokam48.presence.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.kfokam48.presence.dto.ErreurDto;

/** Unique traduction des erreurs en { code, message } (B4). */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ExceptionMetier.class)
    public ResponseEntity<ErreurDto> metier(ExceptionMetier exception) {
        return ResponseEntity.status(exception.getStatut())
                .body(new ErreurDto(exception.getCode(), exception.getMessage()));
    }
}
