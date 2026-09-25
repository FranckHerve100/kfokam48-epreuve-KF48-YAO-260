package com.kfokam48.presence.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.kfokam48.presence.dto.ErreurDto;
import com.kfokam48.presence.dto.RelectureAssigneeDto;
import com.kfokam48.presence.service.RelectureService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/etudiants")
@Tag(name = "Étudiants", description = "Vues propres à un étudiant")
public class EtudiantController {

    private final RelectureService relectureService;

    public EtudiantController(RelectureService relectureService) {
        this.relectureService = relectureService;
    }

    @GetMapping("/{id}/relectures")
    @Operation(summary = "Relectures assignées à un étudiant, en attente d'abord (écran relecteur)")
    @ApiResponse(responseCode = "200", description = "Relectures : EN_ATTENTE ou RENDUE")
    @ApiResponse(responseCode = "404", description = "ETUDIANT_INCONNU",
            content = @Content(schema = @Schema(implementation = ErreurDto.class)))
    public List<RelectureAssigneeDto> relectures(@PathVariable Long id) {
        return relectureService.relecturesDe(id);
    }
}
