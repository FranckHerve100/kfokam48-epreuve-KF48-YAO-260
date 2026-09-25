package com.kfokam48.test;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kfokam48.presence.exception.CodeExpireException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Contrôleur de test, hors du paquet scanné : importé explicitement par ErreursIT pour provoquer
 * chaque famille d'erreur sans dépendre d'une story.
 */
@RestController
public class ControleurDeTest {

    public record Demande(@NotBlank(message = "CHAMP_MANQUANT") String titre,
                          @NotNull(message = "CHAMP_MANQUANT") Long promotionId) {
    }

    @PostMapping("/test/validation")
    @ResponseStatus(HttpStatus.CREATED)
    public Demande valider(@Valid @RequestBody Demande demande) {
        return demande;
    }

    @GetMapping("/test/parametre")
    public long parametre(@RequestParam long valeur) {
        return valeur;
    }

    @GetMapping("/test/metier")
    public void metier() {
        throw new CodeExpireException();
    }

    @GetMapping("/test/panne")
    public void panne() {
        throw new IllegalStateException("détail interne à ne jamais renvoyer");
    }
}
