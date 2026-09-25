package com.kfokam48.presence.config;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Hasard injecté : génération du code de session (H5) et tirage du relecteur (RG10).
 * SecureRandom rend le code difficile à deviner ; les tests injectent un générateur déterministe.
 */
@Configuration
public class RandomConfig {

    @Bean
    public RandomGenerator randomGenerator() {
        return new SecureRandom();
    }
}
