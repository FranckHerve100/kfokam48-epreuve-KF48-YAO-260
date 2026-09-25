package com.kfokam48.presence.contrat;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.test.web.servlet.ResultMatcher;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.mockmvc.OpenApiValidationMatchers;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;

/** Accès au contrat api/contrat.yaml depuis les tests : validation Atlassian et liste des opérations. */
public final class Contrat {

    /** Chemin du contrat, relatif au module backend (répertoire de travail des tests Maven). */
    public static final String EMPLACEMENT = Path.of("..", "api", "contrat.yaml").toAbsolutePath().normalize().toUri().toString();

    private static final OpenApiInteractionValidator VALIDATEUR =
            OpenApiInteractionValidator.createForSpecificationUrl(EMPLACEMENT).build();

    private Contrat() {
    }

    /** Vérifie que la requête et la réponse MockMvc respectent le contrat (statut, corps, schémas). */
    public static ResultMatcher conforme() {
        return OpenApiValidationMatchers.openApi().isValid(VALIDATEUR);
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
