package com.kfokam48.presence.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kfokam48.presence.dto.ErreurDto;
import com.kfokam48.presence.dto.OuvertureSessionDemande;
import com.kfokam48.presence.dto.SessionOuverteDto;
import com.kfokam48.presence.service.SessionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sessions")
@Tag(name = "Sessions", description = "Ouverture des sessions de cours et de leur code de présence")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Le formateur ouvre une session et obtient un code de présence (EF1, opération imposée)",
            description = "Le code (6 caractères, sans I, O, 0, 1) expire 15 minutes après l'ouverture (RG1) "
                    + "et n'est porté par aucune autre session en cours (H5).")
    @ApiResponse(responseCode = "201", description = "Session ouverte")
    @ApiResponse(responseCode = "400", description = "CHAMP_MANQUANT, REQUETE_INVALIDE, PROMOTION_INCONNUE",
            content = @Content(schema = @Schema(implementation = ErreurDto.class)))
    public SessionOuverteDto ouvrir(@Valid @RequestBody OuvertureSessionDemande demande) {
        return sessionService.ouvrir(demande);
    }
}
