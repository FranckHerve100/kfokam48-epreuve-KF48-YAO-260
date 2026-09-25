package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;

import com.kfokam48.presence.domain.Etudiant;
import com.kfokam48.presence.domain.Presence;
import com.kfokam48.presence.domain.PresenceEnregistree;
import com.kfokam48.presence.domain.Promotion;
import com.kfokam48.presence.domain.Session;
import com.kfokam48.presence.domain.SourcePresence;

class RelanceTirageApresPresenceTest {

    private final AssignationService assignation = mock(AssignationService.class);
    private final RelanceTirageApresPresence relance = new RelanceTirageApresPresence(assignation);
    private final Promotion promotion = new Promotion("KF48 Yaoundé");
    private final PresenceEnregistree evenement = new PresenceEnregistree(new Presence(
            new Session("Cours", promotion, "AB23CD", Instant.now()), new Etudiant("Boris", promotion),
            SourcePresence.ETUDIANT, Instant.now()));

    @Test
    void NR_50_presenceValidee_relanceLeTirage() {
        relance.apresPresence(evenement);

        verify(assignation).presenceEnregistree(evenement);
    }

    @Test
    void NR_50_tirageConcurrentEnConflit_nePropagePasLErreur() {
        doThrow(new DataIntegrityViolationException("uk_relecture_exercice")).when(assignation).presenceEnregistree(evenement);

        assertThatCode(() -> relance.apresPresence(evenement)).doesNotThrowAnyException();
    }

    @Test
    void NR_50_verrouDuTirageExpire_nePropagePasLErreur() {
        doThrow(new PessimisticLockingFailureException("verrou")).when(assignation).presenceEnregistree(evenement);

        assertThatCode(() -> relance.apresPresence(evenement)).doesNotThrowAnyException();
    }
}
