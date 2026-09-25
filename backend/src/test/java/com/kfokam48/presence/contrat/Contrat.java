package com.kfokam48.presence.contrat;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.test.web.servlet.ResultMatcher;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers;
import com.atlassian.oai.validator.report.LevelResolver;
import com.atlassian.oai.validator.report.ValidationReport;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;

/** Accès au contrat api/contrat.yaml depuis les tests : validation Atlassian et liste des opérations. */
public final class Contrat {

    /** Chemin du contrat, relatif au module backend (répertoire de travail des tests Maven). */
    public static final String EMPLACEMENT = Path.of("..", "api", "contrat.yaml").toAbsolutePath().normalize().toUri().toString();

    private static final OpenApiInteractionValidator VALIDATEUR =
            OpenApiInteractionValidator.createForSpecificationUrl(EMPLACEMENT).build();

    /** Même contrat, mais la requête n'est pas validée : pour les cas d'erreur, où elle est invalide exprès. */
    private static final OpenApiInteractionValidator VALIDATEUR_REPONSE =
            OpenApiInteractionValidator.createForSpecificationUrl(EMPLACEMENT)
                    .withLevelResolver(LevelResolver.create()
                            .withLevel("validation.request", ValidationReport.Level.IGNORE)
                            .build())
                    .build();

    private Contrat() {
    }

    /** Vérifie que la requête et la réponse MockMvc respectent le contrat (statut, corps, schémas). */
    public static ResultMatcher conforme() {
        return OpenApiValidationMatchers.openApi().isValid(VALIDATEUR);
    }

    /** Vérifie seulement la réponse (statut déclaré, schéma { code, message }) : requête volontairement invalide. */
    public static ResultMatcher reponseConforme() {
        return OpenApiValidationMatchers.openApi().isValid(VALIDATEUR_REPONSE);
    }

    /** Opérations du contrat sous la forme « VERBE /chemin ». */
    public static Set<String> operations() {
        OpenAPI api = new OpenAPIV3Parser().read(EMPLACEMENT);
        Set<String> operations = new LinkedHashSet<>();
        for (Map.Entry<String, PathItem> chemin : api.getPaths().entrySet()) {
            chemin.getValue().readOperationsMap().keySet()
                    .forEach(verbe -> operations.add(verbe.name() + " " + chemin.getKey()));
        }
        return operations;
    }
}
