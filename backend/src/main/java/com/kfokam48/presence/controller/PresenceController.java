package com.kfokam48.presence.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kfokam48.presence.dto.ErreurDto;
import com.kfokam48.presence.dto.MarquagePresenceDemande;
import com.kfokam48.presence.dto.PresenceDto;
import com.kfokam48.presence.service.PresenceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/presences")
@Tag(name = "Présences", description = "Pointage des étudiants avec le code de la session")
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Un étudiant marque sa présence avec le code (EF2, opération imposée)",
            description = "Contrôles dans l'ordre du diagramme D3. Le code est accepté en minuscules.")
    @ApiResponse(responseCode = "201", description = "Présence enregistrée, source ETUDIANT")
    @ApiResponse(responseCode = "400", description = "CODE_INCONNU, CHAMP_MANQUANT, REQUETE_INVALIDE, ETUDIANT_INCONNU, HORS_PROMOTION",
            content = @Content(schema = @Schema(implementation = ErreurDto.class)))
    @ApiResponse(responseCode = "409", description = "DEJA_PRESENT",
            content = @Content(schema = @Schema(implementation = ErreurDto.class)))
    @ApiResponse(responseCode = "410", description = "CODE_EXPIRE (code expiré ou session clôturée)",
            content = @Content(schema = @Schema(implementation = ErreurDto.class)))
    public PresenceDto marquer(@Valid @RequestBody MarquagePresenceDemande demande) {
        return presenceService.marquer(demande);
    }
}
