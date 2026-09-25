package com.kfokam48.presence.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** Un dépôt et une nouvelle présence publient un événement au save : il déclenche le tirage (EF4, H1). */
class EvenementsDomaineTest {

    private final Promotion promotion = new Promotion("KF48 Yaoundé");
    private final Session session = new Session("Cours", promotion, "AB23CD", Instant.parse("2026-09-25T10:00:00Z"));
    private final Etudiant etudiant = new Etudiant("Abena", promotion);

    @Test
    void EF4_nouvelExercice_publieExerciceDepose() {
        Exercice exercice = new Exercice(session, etudiant, "https://github.com/a/b", Instant.now());

        assertThat(evenements(exercice)).containsExactly(new ExerciceDepose(exercice));
    }

    @Test
    void H1_nouvellePresence_publiePresenceEnregistree() {
        Presence presence = new Presence(session, etudiant, SourcePresence.ETUDIANT, Instant.now());

        assertThat(evenements(presence)).containsExactly(new PresenceEnregistree(presence));
    }

    @SuppressWarnings("unchecked")
    private static java.util.Collection<Object> evenements(Object agregat) {
        return (java.util.Collection<Object>) ReflectionTestUtils.invokeMethod(agregat, "domainEvents");
    }
}
