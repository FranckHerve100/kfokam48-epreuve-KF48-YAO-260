package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;

class GenerateurCodeTest {

    @Test
    void H5_code_faitSixCaracteresSansCaractereAmbigu() {
        GenerateurCode generateur = new GenerateurCode(RandomGenerator.of("L64X128MixRandom"));

        for (int i = 0; i < 1_000; i++) {
            assertThat(generateur.nouveauCode()).matches("[A-HJ-NP-Z2-9]{6}");
        }
    }

    @Test
    void H5_alphabet_exclutIO01EtContient32Symboles() {
        assertThat(GenerateurCode.ALPHABET).hasSize(32).doesNotContain("I", "O", "0", "1");
    }

    @Test
    void H5_codes_sontVariesAvecUnHasardReel() {
        GenerateurCode generateur = new GenerateurCode(RandomGenerator.of("L64X128MixRandom"));
        Set<String> codes = new HashSet<>();

        for (int i = 0; i < 1_000; i++) {
            codes.add(generateur.nouveauCode());
        }

        assertThat(codes).hasSizeGreaterThan(990);
    }
}
