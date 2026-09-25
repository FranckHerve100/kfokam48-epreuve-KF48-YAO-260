package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.kfokam48.presence.domain.Etudiant;
import com.kfokam48.presence.domain.Promotion;
import com.kfokam48.presence.domain.Session;
import com.kfokam48.presence.dto.SessionDto;
import com.kfokam48.presence.exception.PromotionInconnueException;
import com.kfokam48.presence.repository.EtudiantRepository;
import com.kfokam48.presence.repository.PromotionRepository;
import com.kfokam48.presence.repository.SessionRepository;

class ReferentielServiceTest {

    private final PromotionRepository promotions = mock(PromotionRepository.class);
    private final SessionRepository sessions = mock(SessionRepository.class);
    private final EtudiantRepository etudiants = mock(EtudiantRepository.class);
    private final ReferentielService service = new ReferentielService(promotions, sessions, etudiants);

    @Test
    void promotions_sontListeesParNom() {
        when(promotions.findAllByOrderByNomAsc()).thenReturn(List.of(new Promotion("KF48 Douala"), new Promotion("KF48 Yaoundé")));

        assertThat(service.promotions()).extracting("nom").containsExactly("KF48 Douala", "KF48 Yaoundé");
    }

    @Test
    void sessions_dUnePromotion_avecExpirationEtClotureEventuelle() {
        Promotion promotion = new Promotion("KF48 Yaoundé");
        Instant dixHeures = Instant.parse("2026-09-25T10:00:00Z");
        when(promotions.existsById(1L)).thenReturn(true);
        when(sessions.findByPromotionIdOrderByOuvertureAtDesc(1L))
                .thenReturn(List.of(new Session("Cours 1", promotion, "AB23CD", dixHeures)));

        List<SessionDto> resultat = service.sessionsDeLaPromotion(1L);

        assertThat(resultat).singleElement().satisfies(s -> {
            assertThat(s.code()).isEqualTo("AB23CD");
            assertThat(s.expirationAt()).isEqualTo(Instant.parse("2026-09-25T10:15:00Z"));
            assertThat(s.clotureAt()).isNull();
        });
    }

    @Test
    void sessions_dUnePromotionInconnue_renvoie404PromotionInconnue() {
        when(promotions.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.sessionsDeLaPromotion(99L))
                .isInstanceOfSatisfying(PromotionInconnueException.class,
                        e -> assertThat(e.getStatut()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void Q1_etudiantsDUnePromotion_sontListesParNom() {
        Promotion promotion = new Promotion("KF48 Yaoundé");
        when(promotions.existsById(1L)).thenReturn(true);
        when(etudiants.findByPromotionIdOrderByNomAsc(1L))
                .thenReturn(List.of(new Etudiant("Abena Mvondo", promotion), new Etudiant("Boris Ngono", promotion)));

        assertThat(service.etudiantsDeLaPromotion(1L)).extracting("nom").containsExactly("Abena Mvondo", "Boris Ngono");
    }

    @Test
    void Q1_etudiantsDUnePromotionInconnue_renvoie404PromotionInconnue() {
        when(promotions.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.etudiantsDeLaPromotion(99L))
                .isInstanceOfSatisfying(PromotionInconnueException.class,
                        e -> assertThat(e.getStatut()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
