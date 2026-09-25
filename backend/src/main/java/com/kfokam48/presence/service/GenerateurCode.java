package com.kfokam48.presence.service;

import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

/**
 * Code de présence : 6 caractères tirés parmi 32 symboles sans ambiguïté à l'écran
 * (ni I, ni O, ni 0, ni 1), soit environ un milliard de combinaisons (H5).
 */
@Component
public class GenerateurCode {

    static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    static final int LONGUEUR = 6;

    private final RandomGenerator hasard;

    public GenerateurCode(RandomGenerator hasard) {
        this.hasard = hasard;
    }

    public String nouveauCode() {
        StringBuilder code = new StringBuilder(LONGUEUR);
        for (int i = 0; i < LONGUEUR; i++) {
            code.append(ALPHABET.charAt(hasard.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
