package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.kfokam48.presence.domain.Etudiant;
import com.kfokam48.presence.domain.Exercice;
import com.kfokam48.presence.domain.Promotion;
import com.kfokam48.presence.domain.Session;
import com.kfokam48.presence.domain.StatutExercice;
import com.kfokam48.presence.dto.DepotExerciceDemande;
import com.kfokam48.presence.dto.ExerciceDeposeDto;
import com.kfokam48.presence.exception.EtudiantInconnuException;
import com.kfokam48.presence.exception.ExerciceDejaDeposeException;
import com.kfokam48.presence.exception.HorsPromotionException;
import com.kfokam48.presence.exception.LienInvalideException;
import com.kfokam48.presence.exception.SessionClotureeException;
import com.kfokam48.presence.exception.SessionInconnueException;
import com.kfokam48.presence.repository.EtudiantRepository;
import com.kfokam48.presence.repository.ExerciceRepository;
import com.kfokam48.presence.repository.SessionRepository;

class ExerciceServiceTest {

    private static final Instant OUVERTURE = Instant.parse("2026-09-25T10:00:00Z");
    private static final String LIEN = "https://github.com/abena/kf48-exercice";

    private final SessionRepository sessions = mock(SessionRepository.class);
    private final EtudiantRepository etudiants = mock(EtudiantRepository.class);
    private final ExerciceRepository exercices = mock(ExerciceRepository.class);

    private final Promotion yaounde = avecId(new Promotion("KF48 Yaoundé"), 1L);
    private final Etudiant abena = avecId(new Etudiant("Abena", yaounde), 10L);
    private final Etudiant grace = avecId(new Etudiant("Grace", avecId(new Promotion("KF48 Douala"), 2L)), 20L);
    private final Session session = avecId(new Session("Cours", yaounde, "AB23CD", OUVERTURE), 100L);

    @BeforeEach
    void preparer() {
        when(sessions.findById(100L)).thenReturn(Optional.of(session));
        when(etudiants.findById(10L)).thenReturn(Optional.of(abena));
        when(etudiants.findById(20L)).thenReturn(Optional.of(grace));
        when(exercices.saveAndFlush(any(Exercice.class))).thenAnswer(appel -> avecId(appel.getArgument(0), 500L));
    }

    @Test
    void EF3_depotSurSessionOuverte_renvoieExerciceDepose() {
        ExerciceDeposeDto depose = service(Duration.ofMinutes(5)).deposer(new DepotExerciceDemande(100L, 10L, LIEN));

        assertThat(depose).isEqualTo(new ExerciceDeposeDto(500L, StatutExercice.DEPOSE));
    }

    @Test
    void RG7_codeExpireMaisSessionNonCloturee_depotAccepte() {
        ExerciceDeposeDto depose = service(Duration.ofHours(6)).deposer(new DepotExerciceDemande(100L, 10L, LIEN));

        assertThat(depose.statut()).isEqualTo(StatutExercice.DEPOSE);
    }

    @Test
    void RG7_sessionCloturee_renvoie409SessionCloturee() {
        session.cloturer(OUVERTURE.plus(Duration.ofHours(2)));
        ExerciceService service = service(Duration.ofHours(3));
        DepotExerciceDemande demande = new DepotExerciceDemande(100L, 10L, LIEN);

        assertThatThrownBy(() -> service.deposer(demande)).isInstanceOf(SessionClotureeException.class);
        verify(exercices, never()).saveAndFlush(any());
    }

    @Test
    void RG6_secondDepot_renvoie409ExerciceDejaDepose() {
        when(exercices.existsBySessionIdAndEtudiantId(100L, 10L)).thenReturn(true);
        ExerciceService service = service(Duration.ofMinutes(5));
        DepotExerciceDemande demande = new DepotExerciceDemande(100L, 10L, LIEN);

        assertThatThrownBy(() -> service.deposer(demande)).isInstanceOf(ExerciceDejaDeposeException.class);
        verify(exercices, never()).saveAndFlush(any());
    }

    @Test
    void RG6_doublonDetecteParLaBase_renvoie409() {
        when(exercices.saveAndFlush(any(Exercice.class))).thenThrow(new DataIntegrityViolationException("uk_exercice"));
        ExerciceService service = service(Duration.ofMinutes(5));
        DepotExerciceDemande demande = new DepotExerciceDemande(100L, 10L, LIEN);

        assertThatThrownBy(() -> service.deposer(demande)).isInstanceOf(ExerciceDejaDeposeException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "github.com/abena", "ftp://serveur/exercice", "https://", "http:///chemin", "https://exemple .com"})
    void RG16_lienInvalide_renvoie400LienInvalide(String lien) {
        ExerciceService service = service(Duration.ofMinutes(5));
        DepotExerciceDemande demande = new DepotExerciceDemande(100L, 10L, lien);

        assertThatThrownBy(() -> service.deposer(demande)).isInstanceOf(LienInvalideException.class);
        verify(sessions, never()).findById(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://github.com/abena/kf48", "http://gitlab.local:8080/a/b?x=1#y", "HTTPS://GitHub.com/A"})
    void RG16_lienHttpOuHttps_accepte(String lien) {
        assertThat(service(Duration.ofMinutes(5)).deposer(new DepotExerciceDemande(100L, 10L, lien)).id()).isEqualTo(500L);
    }

    @Test
    void RG16_lienDePlusDe2000Caracteres_renvoie400LienInvalide() {
        ExerciceService service = service(Duration.ofMinutes(5));
        DepotExerciceDemande demande = new DepotExerciceDemande(100L, 10L, "https://exemple.com/" + "a".repeat(2000));

        assertThatThrownBy(() -> service.deposer(demande)).isInstanceOf(LienInvalideException.class);
    }

    @Test
    void EF3_sessionInconnue_renvoie400SessionInconnue() {
        when(sessions.findById(999L)).thenReturn(Optional.empty());
        ExerciceService service = service(Duration.ofMinutes(5));
        DepotExerciceDemande demande = new DepotExerciceDemande(999L, 10L, LIEN);

        assertThatThrownBy(() -> service.deposer(demande))
                .isInstanceOfSatisfying(SessionInconnueException.class,
                        e -> assertThat(e.getStatut()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void EF3_etudiantInconnu_renvoie400EtudiantInconnu() {
        when(etudiants.findById(99L)).thenReturn(Optional.empty());
        ExerciceService service = service(Duration.ofMinutes(5));
        DepotExerciceDemande demande = new DepotExerciceDemande(100L, 99L, LIEN);

        assertThatThrownBy(() -> service.deposer(demande)).isInstanceOf(EtudiantInconnuException.class);
    }

    @Test
    void RG18_etudiantAutrePromotion_renvoie400HorsPromotion() {
        ExerciceService service = service(Duration.ofMinutes(5));
        DepotExerciceDemande demande = new DepotExerciceDemande(100L, 20L, LIEN);

        assertThatThrownBy(() -> service.deposer(demande)).isInstanceOf(HorsPromotionException.class);
    }

    @Test
    void H8_depotSansPresence_accepte() {
        assertThat(service(Duration.ofMinutes(5)).deposer(new DepotExerciceDemande(100L, 10L, LIEN)).statut())
                .isEqualTo(StatutExercice.DEPOSE);
    }

    private ExerciceService service(Duration apresOuverture) {
        return new ExerciceService(sessions, etudiants, exercices,
                Clock.fixed(OUVERTURE.plus(apresOuverture), ZoneOffset.UTC));
    }

    private static <T> T avecId(T entite, Long id) {
        ReflectionTestUtils.setField(entite, "id", id);
        return entite;
    }
}
