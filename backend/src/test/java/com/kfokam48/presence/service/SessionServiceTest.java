package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.kfokam48.presence.domain.Promotion;
import com.kfokam48.presence.domain.Session;
import com.kfokam48.presence.dto.OuvertureSessionDemande;
import com.kfokam48.presence.dto.SessionOuverteDto;
import com.kfokam48.presence.exception.PromotionInconnueException;
import com.kfokam48.presence.repository.PromotionRepository;
import com.kfokam48.presence.repository.SessionRepository;

class SessionServiceTest {

    private static final Instant DIX_HEURES = Instant.parse("2026-09-25T10:00:00Z");

    private final SessionRepository sessions = mock(SessionRepository.class);
    private final PromotionRepository promotions = mock(PromotionRepository.class);
    private final GenerateurCode generateur = mock(GenerateurCode.class);
    private final Promotion promotion = new Promotion("KF48 Yaoundé");
    private SessionService service;

    @BeforeEach
    void preparer() {
        service = new SessionService(sessions, promotions, generateur, Clock.fixed(DIX_HEURES, ZoneOffset.UTC));
        when(promotions.findById(1L)).thenReturn(Optional.of(promotion));
        when(sessions.save(any(Session.class))).thenAnswer(appel -> appel.getArgument(0));
    }

    @Test
    void RG1_sessionOuverteA10h_expireA10h15() {
        when(generateur.nouveauCode()).thenReturn("AB23CD");

        SessionOuverteDto session = service.ouvrir(new OuvertureSessionDemande("Cours 1", 1L));

        assertThat(session.ouvertureAt()).isEqualTo(DIX_HEURES);
        assertThat(session.expirationAt()).isEqualTo(Instant.parse("2026-09-25T10:15:00Z"));
        assertThat(session.code()).isEqualTo("AB23CD");
    }

    @Test
    void H5_codeDejaPrisParUneSessionNonExpiree_unAutreCodeEstTire() {
        when(generateur.nouveauCode()).thenReturn("PRIS22", "LIBRE3");
        when(sessions.existsByCodeAndExpirationAtGreaterThanEqual("PRIS22", DIX_HEURES)).thenReturn(true);
        when(sessions.existsByCodeAndExpirationAtGreaterThanEqual("LIBRE3", DIX_HEURES)).thenReturn(false);

        SessionOuverteDto session = service.ouvrir(new OuvertureSessionDemande("Cours 1", 1L));

        assertThat(session.code()).isEqualTo("LIBRE3");
    }

    @Test
    void H5_aucunCodeLibreApresVingtTirages_erreurInterne() {
        when(generateur.nouveauCode()).thenReturn("PRIS22");
        when(sessions.existsByCodeAndExpirationAtGreaterThanEqual(eq("PRIS22"), any())).thenReturn(true);

        assertThatThrownBy(() -> service.ouvrir(new OuvertureSessionDemande("Cours 1", 1L)))
                .isInstanceOf(IllegalStateException.class);
        verify(sessions, never()).save(any());
    }

    @Test
    void EF1_promotionInconnue_renvoie400PromotionInconnue() {
        when(promotions.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ouvrir(new OuvertureSessionDemande("Cours 1", 99L)))
                .isInstanceOfSatisfying(PromotionInconnueException.class, e -> {
                    assertThat(e.getStatut()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("PROMOTION_INCONNUE");
                });
        verify(sessions, never()).save(any());
    }

    @Test
    void EF1_titre_estEnregistreSansEspacesAutour() {
        when(generateur.nouveauCode()).thenReturn("AB23CD");

        service.ouvrir(new OuvertureSessionDemande("  Cours 1  ", 1L));

        verify(sessions).save(org.mockito.ArgumentMatchers.argThat(s -> s.getTitre().equals("Cours 1")));
    }
}
