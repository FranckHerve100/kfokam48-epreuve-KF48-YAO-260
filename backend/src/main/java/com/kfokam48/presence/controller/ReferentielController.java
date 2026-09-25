package com.kfokam48.presence.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kfokam48.presence.dto.ErreurDto;
import com.kfokam48.presence.dto.ReferenceDto;
import com.kfokam48.presence.dto.SessionDto;
import com.kfokam48.presence.service.ReferentielService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/promotions")
@Tag(name = "Référentiel", description = "Promotions, sessions et étudiants affichés dans les listes des écrans")
public class ReferentielController {

    private final ReferentielService referentielService;

    public ReferentielController(ReferentielService referentielService) {
        this.referentielService = referentielService;
    }

    @GetMapping
    @Operation(summary = "Liste des promotions, triées par nom")
    @ApiResponse(responseCode = "200", description = "Promotions")
    public List<ReferenceDto> promotions() {
        return referentielService.promotions();
    }

    @GetMapping("/{id}/sessions")
    @Operation(summary = "Sessions d'une promotion, la plus récente en premier")
    @ApiResponse(responseCode = "200", description = "Sessions ; clotureAt vaut null tant que la session n'est pas clôturée")
    @ApiResponse(responseCode = "404", description = "PROMOTION_INCONNUE",
            content = @Content(schema = @Schema(implementation = ErreurDto.class)))
    public List<SessionDto> sessions(@PathVariable Long id) {
        return referentielService.sessionsDeLaPromotion(id);
    }
}
