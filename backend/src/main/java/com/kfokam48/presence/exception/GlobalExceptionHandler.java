package com.kfokam48.presence.exception;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.kfokam48.presence.dto.ErreurDto;

/**
 * Unique traduction des erreurs en { code, message } (B4, RG17). Aucune stack trace, aucun détail
 * technique n'est renvoyé : les erreurs inattendues sont journalisées côté serveur seulement.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String CHAMP_MANQUANT = "CHAMP_MANQUANT";
    static final String REQUETE_INVALIDE = "REQUETE_INVALIDE";

    /**
     * Messages des codes portés par les annotations de validation des DTO
     * ({@code @NotNull(message = "CHAMP_MANQUANT")}, {@code @Min(message = "NOTE_INVALIDE")}…).
     */
    private static final Map<String, String> MESSAGES_VALIDATION = Map.of(
            CHAMP_MANQUANT, "Un champ obligatoire est absent : %s.",
            "NOTE_INVALIDE", new NoteInvalideException().getMessage(),
            "LIEN_INVALIDE", new LienInvalideException().getMessage());

    @ExceptionHandler(ExceptionMetier.class)
    public ResponseEntity<ErreurDto> metier(ExceptionMetier exception) {
        return ResponseEntity.status(exception.getStatut())
                .body(new ErreurDto(exception.getCode(), exception.getMessage()));
    }

    /** Bean Validation : CHAMP_MANQUANT en priorité, sinon le code porté par l'annotation (RG17). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErreurDto> validation(MethodArgumentNotValidException exception) {
        List<org.springframework.validation.FieldError> erreurs = exception.getBindingResult().getFieldErrors();
        org.springframework.validation.FieldError retenue = erreurs.stream()
                .filter(erreur -> CHAMP_MANQUANT.equals(erreur.getDefaultMessage()))
                .findFirst()
                .orElse(erreurs.isEmpty() ? null : erreurs.get(0));
        if (retenue == null || !MESSAGES_VALIDATION.containsKey(retenue.getDefaultMessage())) {
            return erreur(HttpStatus.BAD_REQUEST, REQUETE_INVALIDE, "La requête est invalide.");
        }
        String code = retenue.getDefaultMessage();
        return erreur(HttpStatus.BAD_REQUEST, code, MESSAGES_VALIDATION.get(code).formatted(retenue.getField()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErreurDto> illisible(HttpMessageNotReadableException exception) {
        return erreur(HttpStatus.BAD_REQUEST, REQUETE_INVALIDE,
                "Le corps de la requête est absent, n'est pas un JSON valide ou contient un champ mal typé.");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MissingRequestHeaderException.class})
    public ResponseEntity<ErreurDto> parametreManquant(Exception exception) {
        return erreur(HttpStatus.BAD_REQUEST, REQUETE_INVALIDE, "Un paramètre obligatoire est absent.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErreurDto> parametreMalType(MethodArgumentTypeMismatchException exception) {
        return erreur(HttpStatus.BAD_REQUEST, REQUETE_INVALIDE,
                "Le paramètre « %s » n'a pas le bon format.".formatted(exception.getName()));
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ResponseEntity<ErreurDto> routeInconnue(Exception exception) {
        return erreur(HttpStatus.NOT_FOUND, "RESSOURCE_INCONNUE", "Cette adresse n'existe pas dans l'API.");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErreurDto> verbeNonSupporte(HttpRequestMethodNotSupportedException exception) {
        return erreur(HttpStatus.METHOD_NOT_ALLOWED, REQUETE_INVALIDE,
                "La méthode %s n'est pas acceptée sur cette adresse.".formatted(exception.getMethod()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErreurDto> typeNonSupporte(HttpMediaTypeNotSupportedException exception) {
        return erreur(HttpStatus.UNSUPPORTED_MEDIA_TYPE, REQUETE_INVALIDE,
                "Le corps de la requête doit être du JSON (Content-Type: application/json).");
    }

    /** Toute autre exception : journalisée, jamais détaillée au client. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErreurDto> inattendue(Exception exception) {
        LOG.error("Erreur inattendue", exception);
        return erreur(HttpStatus.INTERNAL_SERVER_ERROR, "ERREUR_INTERNE",
                "Une erreur interne est survenue. Réessayez plus tard.");
    }

    private static ResponseEntity<ErreurDto> erreur(HttpStatus statut, String code, String message) {
        return ResponseEntity.status(statut).body(new ErreurDto(code, message));
    }
}
