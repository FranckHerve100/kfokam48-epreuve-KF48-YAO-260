package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import com.kfokam48.presence.domain.Etudiant;
import com.kfokam48.presence.domain.Presence;
import com.kfokam48.presence.domain.Promotion;
import com.kfokam48.presence.domain.Session;
import com.kfokam48.presence.domain.SourcePresence;
import com.kfokam48.presence.dto.MarquagePresenceDemande;
import com.kfokam48.presence.dto.PresenceDto;
import com.kfokam48.presence.exception.CodeExpireException;
import com.kfokam48.presence.exception.CodeInconnuException;
import com.kfokam48.presence.exception.DejaPresentException;
import com.kfokam48.presence.exception.EtudiantInconnuException;
import com.kfokam48.presence.exception.HorsPromotionException;
import com.kfokam48.presence.repository.EtudiantRepository;
import com.kfokam48.presence.repository.PresenceRepository;
import com.kfokam48.presence.repository.SessionRepository;

/** POST /api/presences dans l'ordre des contrôles de D3 (RG4 hors périmètre de l'étape 2). */
class PresenceServiceTest {

    private static final Instant OUVERTURE = Instant.parse("2026-09-25T10:00:00Z");

    private final EtudiantRepository etudiants = mock(EtudiantRepository.class);
    private final SessionRepository sessions = mock(SessionRepository.class);
    private final PresenceRepository presences = mock(PresenceRepository.class);

    private final Promotion yaounde = avecId(new Promotion("KF48 Yaoundé"), 1L);
    private final Promotion douala = avecId(new Promotion("KF48 Douala"), 2L);
    private final Etudiant abena = avecId(new Etudiant("Abena", yaounde), 10L);
    private final Etudiant grace = avecId(new Etudiant("Grace", douala), 20L);
    private final Session session = avecId(new Session("Cours", yaounde, "AB23CD", OUVERTURE), 100L);

    @BeforeEach
    void preparer() {
        when(etudiants.findById(10L)).thenReturn(Optional.of(abena));
        when(etudiants.findById(20L)).thenReturn(Optional.of(grace));
        when(sessions.findFirstByCodeOrderByOuvertureAtDesc("AB23CD")).thenReturn(Optional.of(session));
        when(presences.saveAndFlush(any(Presence.class))).thenAnswer(appel -> avecId(appel.getArgument(0), 1000L));
    }

    @Test
    void EF2_codeValideCinqMinutesApresOuverture_creePresenceSourceEtudiant() {
        PresenceDto presence = service(5).marquer(new MarquagePresenceDemande("AB23CD", 10L));

        assertThat(presence).isEqualTo(new PresenceDto(1000L, 100L, 10L, SourcePresence.ETUDIANT));
        verify(presences).saveAndFlush(org.mockito.ArgumentMatchers.argThat(p ->
                p.getMarqueeAt().equals(OUVERTURE.plus(Duration.ofMinutes(5)))));
    }

    @Test
    void RG1_codeAExactementQuinzeMinutes_presenceAcceptee() {
        assertThat(service(15).marquer(new MarquagePresenceDemande("AB23CD", 10L)).source())
                .isEqualTo(SourcePresence.ETUDIANT);
    }

    @Test
    void RG1_codeExpireApres15Minutes_renvoie410() {
        PresenceService service = service(16);
        MarquagePresenceDemande demande = new MarquagePresenceDemande("AB23CD", 10L);

        assertThatThrownBy(() -> service.marquer(demande)).isInstanceOf(CodeExpireException.class);
        verify(presences, never()).saveAndFlush(any());
    }

    @Test
    void H2_sessionClotureeAvantExpiration_renvoie410() {
        session.cloturer(OUVERTURE.plus(Duration.ofMinutes(3)));
        PresenceService service = service(5);
        MarquagePresenceDemande demande = new MarquagePresenceDemande("AB23CD", 10L);

        assertThatThrownBy(() -> service.marquer(demande)).isInstanceOf(CodeExpireException.class);
    }

    @Test
    void RG2_dejaPresent_renvoie409() {
        when(presences.existsBySessionIdAndEtudiantId(100L, 10L)).thenReturn(true);
        PresenceService service = service(5);
        MarquagePresenceDemande demande = new MarquagePresenceDemande("AB23CD", 10L);

        assertThatThrownBy(() -> service.marquer(demande)).isInstanceOf(DejaPresentException.class);
        verify(presences, never()).saveAndFlush(any());
    }

    @Test
    void RG2_doublonDetecteParLaBase_renvoie409() {
        when(presences.saveAndFlush(any(Presence.class))).thenThrow(new DataIntegrityViolationException("uk_presence_session_etudiant"));
        PresenceService service = service(5);
        MarquagePresenceDemande demande = new MarquagePresenceDemande("AB23CD", 10L);

        assertThatThrownBy(() -> service.marquer(demande)).isInstanceOf(DejaPresentException.class);
    }

    @Test
    void RG3_codeInconnu_renvoie400() {
        when(sessions.findFirstByCodeOrderByOuvertureAtDesc(anyString())).thenReturn(Optional.empty());
        PresenceService service = service(5);
        MarquagePresenceDemande demande = new MarquagePresenceDemande("ZZZZZZ", 10L);

        assertThatThrownBy(() -> service.marquer(demande)).isInstanceOf(CodeInconnuException.class);
    }

    @Test
    void RG18_etudiantAutrePromotion_renvoie400() {
        PresenceService service = service(5);
        MarquagePresenceDemande demande = new MarquagePresenceDemande("AB23CD", 20L);

        assertThatThrownBy(() -> service.marquer(demande)).isInstanceOf(HorsPromotionException.class);
    }

    @Test
    void EF2_etudiantInconnu_renvoie400AvantDeRegarderLeCode() {
        when(etudiants.findById(99L)).thenReturn(Optional.empty());
        PresenceService service = service(5);
        MarquagePresenceDemande demande = new MarquagePresenceDemande("ZZZZZZ", 99L);

        assertThatThrownBy(() -> service.marquer(demande)).isInstanceOf(EtudiantInconnuException.class);
        verify(sessions, never()).findFirstByCodeOrderByOuvertureAtDesc(anyString());
    }

    @Test
    void H5_codeSaisiEnMinusculesAvecEspaces_estReconnu() {
        assertThat(service(5).marquer(new MarquagePresenceDemande("  ab23cd ", 10L)).sessionId()).isEqualTo(100L);
    }

    @Test
    void H5_codeReutiliseParUneSessionPlusRecente_seulLaDerniereCompte() {
        Session recente = avecId(new Session("Cours 2", yaounde, "AB23CD", OUVERTURE.plus(Duration.ofDays(1))), 200L);
        when(sessions.findFirstByCodeOrderByOuvertureAtDesc("AB23CD")).thenReturn(Optional.of(recente));

        PresenceDto presence = new PresenceService(etudiants, sessions, presences,
                Clock.fixed(OUVERTURE.plus(Duration.ofDays(1)).plusSeconds(60), ZoneOffset.UTC))
                .marquer(new MarquagePresenceDemande("AB23CD", 10L));

        assertThat(presence.sessionId()).isEqualTo(200L);
    }

    private PresenceService service(int minutesApresOuverture) {
        Clock horloge = Clock.fixed(OUVERTURE.plus(Duration.ofMinutes(minutesApresOuverture)), ZoneOffset.UTC);
        return new PresenceService(etudiants, sessions, presences, horloge);
    }

    private static <T> T avecId(T entite, Long id) {
        ReflectionTestUtils.setField(entite, "id", id);
        return entite;
    }
}
