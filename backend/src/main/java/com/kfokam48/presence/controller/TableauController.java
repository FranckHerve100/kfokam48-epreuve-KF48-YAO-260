package com.kfokam48.presence.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.kfokam48.presence.dto.ErreurDto;
import com.kfokam48.presence.dto.LigneTableauDto;
import com.kfokam48.presence.service.TableauService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/tableau")
@Tag(name = "Tableau", description = "Suivi d'une promotion par le formateur")
public class TableauController {

    private final TableauService tableauService;

    public TableauController(TableauService tableauService) {
        this.tableauService = tableauService;
    }

    @GetMapping
    @Operation(summary = "Le tableau récapitulatif du formateur (EF6, opération imposée)",
            description = "Une ligne par étudiant ; moyenne des notes reçues calculée par l'API, 2 décimales, null sans note (RG15).")
    @ApiResponse(responseCode = "200", description = "Récapitulatif par étudiant, trié par nom")
    @ApiResponse(responseCode = "404", description = "PROMOTION_INCONNUE (aussi si promotionId est absent ou non numérique)",
            content = @Content(schema = @Schema(implementation = ErreurDto.class)))
    public List<LigneTableauDto> tableau(
            @Parameter(description = "Identifiant de la promotion", required = true, schema = @Schema(type = "integer", format = "int64"))
            @RequestParam(required = false) String promotionId) {
        return tableauService.tableau(promotionId);
    }
}
