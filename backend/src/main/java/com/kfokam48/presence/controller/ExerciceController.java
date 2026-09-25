package com.kfokam48.presence.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kfokam48.presence.dto.DepotExerciceDemande;
import com.kfokam48.presence.dto.ErreurDto;
import com.kfokam48.presence.dto.ExerciceDeposeDto;
import com.kfokam48.presence.service.ExerciceService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/exercices")
@Tag(name = "Exercices", description = "Dépôt du lien des exercices")
public class ExerciceController {

    private final ExerciceService exerciceService;

    public ExerciceController(ExerciceService exerciceService) {
        this.exerciceService = exerciceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Un étudiant dépose le lien de son exercice (EF3, opération imposée)",
            description = "Possible jusqu'à la clôture de la session, même code expiré (RG7). Statut selon le diagramme D4.")
    @ApiResponse(responseCode = "201", description = "Exercice déposé : DEPOSE ou EN_ATTENTE_RELECTURE")
    @ApiResponse(responseCode = "400", description = "LIEN_INVALIDE, CHAMP_MANQUANT, REQUETE_INVALIDE, SESSION_INCONNUE, ETUDIANT_INCONNU, HORS_PROMOTION",
            content = @Content(schema = @Schema(implementation = ErreurDto.class)))
    @ApiResponse(responseCode = "409", description = "EXERCICE_DEJA_DEPOSE, SESSION_CLOTUREE",
            content = @Content(schema = @Schema(implementation = ErreurDto.class)))
    public ExerciceDeposeDto deposer(@Valid @RequestBody DepotExerciceDemande demande) {
        return exerciceService.deposer(demande);
    }
}
