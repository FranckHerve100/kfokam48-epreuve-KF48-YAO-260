package com.kfokam48.presence.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kfokam48.presence.dto.ErreurDto;
import com.kfokam48.presence.dto.RelectureDemande;
import com.kfokam48.presence.service.RelectureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/relectures")
@Tag(name = "Relectures", description = "Notes et commentaires rendus par les relecteurs")
public class RelectureController {

    private final RelectureService relectureService;

    public RelectureController(RelectureService relectureService) {
        this.relectureService = relectureService;
    }

    @PostMapping("/{id}")
    @Operation(summary = "Un relecteur rend sa note et son commentaire (EF5, opération imposée)",
            description = "Note entière de 0 à 20 (RG11), définitive dès l'envoi (RG12). L'exercice passe RELU.")
    @ApiResponse(responseCode = "200", description = "Relecture enregistrée")
    @ApiResponse(responseCode = "400", description = "NOTE_INVALIDE, CHAMP_MANQUANT, REQUETE_INVALIDE, RELECTURE_INCONNUE",
            content = @Content(schema = @Schema(implementation = ErreurDto.class)))
    @ApiResponse(responseCode = "403", description = "AUTO_RELECTURE, RELECTEUR_NON_ASSIGNE",
            content = @Content(schema = @Schema(implementation = ErreurDto.class)))
    @ApiResponse(responseCode = "409", description = "RELECTURE_DEJA_RENDUE",
            content = @Content(schema = @Schema(implementation = ErreurDto.class)))
    public void rendre(@PathVariable Long id, @Valid @RequestBody RelectureDemande demande,
                       @Parameter(description = "Étudiant qui envoie la relecture (H11), facultatif")
                       @RequestHeader(name = "X-Etudiant-Id", required = false) Long etudiantId) {
        relectureService.rendre(id, demande, etudiantId);
    }
}
