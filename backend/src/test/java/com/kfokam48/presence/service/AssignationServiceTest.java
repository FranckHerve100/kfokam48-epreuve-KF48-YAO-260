package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.random.RandomGenerator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import com.kfokam48.presence.domain.Etudiant;
import com.kfokam48.presence.domain.Exercice;
import com.kfokam48.presence.domain.ExerciceDepose;
import com.kfokam48.presence.domain.Presence;
import com.kfokam48.presence.domain.PresenceEnregistree;
import com.kfokam48.presence.domain.Promotion;
import com.kfokam48.presence.domain.Relecture;
import com.kfokam48.presence.domain.Session;
import com.kfokam48.presence.domain.SourcePresence;
import com.kfokam48.presence.domain.StatutExercice;
import com.kfokam48.presence.repository.ExerciceRepository;
import com.kfokam48.presence.repository.PresenceRepository;
import com.kfokam48.presence.repository.RelectureRepository;

/** Tirage du relecteur : un seul par exercice (RG9), parmi les présents sauf l'auteur, le moins chargé d'abord (RG10, H1). */
class AssignationServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-09-25T10:30:00Z");

    private final PresenceRepository presences = mock(PresenceRepository.class);
    private final RelectureRepository relectures = mock(RelectureRepository.class);
    private final ExerciceRepository exercices = mock(ExerciceRepository.class);
    private final RandomGenerator hasard = mock(RandomGenerator.class);
    private final AssignationService service = new AssignationService(presences, relectures, exercices, hasard,
            Clock.fixed(MAINTENANT, ZoneOffset.UTC));

    private final Promotion promotion = avecId(new Promotion("KF48 Yaoundé"), 1L);
    private final Session session = avecId(new Session("Cours", promotion, "AB23CD", MAINTENANT.minusSeconds(1800)), 100L);
    private final Etudiant auteur = avecId(new Etudiant("Abena", promotion), 10L);
    private final Etudiant boris = avecId(new Etudiant("Boris", promotion), 11L);
    private final Etudiant carine = avecId(new Etudiant("Carine", promotion), 12L);
    private Exercice exercice;

    @BeforeEach
    void preparer() {
        exercice = avecId(new Exercice(session, auteur, "https://github.com/abena/ex", MAINTENANT), 500L);
        when(relectures.save(any(Relecture.class))).thenAnswer(appel -> appel.getArgument(0));
    }

    @Test
    void RG10_troisPresentsDontLAuteur_unDesDeuxAutresEstAssigne() {
        presents(auteur, boris, carine);
        when(hasard.nextInt(2)).thenReturn(1);

        service.assigner(exercice);

        assertThat(relecteurAssigne()).isEqualTo(carine);
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.EN_ATTENTE_RELECTURE);
    }

    @Test
    void RG10_jamaisLAuteur_memeSeulCandidatRestant() {
        presents(auteur, boris);
        when(hasard.nextInt(1)).thenReturn(0);

        service.assigner(exercice);

        assertThat(relecteurAssigne()).isEqualTo(boris);
    }

    @Test
    void H1_auteurSeulPresent_resteDeposeSansRelecture() {
        presents(auteur);

        service.assigner(exercice);

        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.DEPOSE);
        verify(relectures, never()).save(any());
    }

    @Test
    void H1_aucunPresent_auteurAbsent_resteDepose() {
        presents();

        service.assigner(exercice);

        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.DEPOSE);
        verify(relectures, never()).save(any());
    }

    @Test
    void RG10_leMoinsChargeEstChoisiEnPremier() {
        presents(auteur, boris, carine);
        when(relectures.countByRelecteurIdAndExerciceSessionId(11L, 100L)).thenReturn(2L);
        when(relectures.countByRelecteurIdAndExerciceSessionId(12L, 100L)).thenReturn(0L);
        when(hasard.nextInt(1)).thenReturn(0);

        service.assigner(exercice);

        assertThat(relecteurAssigne()).isEqualTo(carine);
        verify(hasard, never()).nextInt(2);
    }

    @Test
    void RG10_exAequo_departageAuHasard() {
        presents(auteur, boris, carine);
        when(relectures.countByRelecteurIdAndExerciceSessionId(anyLong(), anyLong())).thenReturn(1L);
        when(hasard.nextInt(2)).thenReturn(0);

        service.assigner(exercice);

        assertThat(relecteurAssigne()).isEqualTo(boris);
        verify(hasard).nextInt(2);
    }

    @Test
    void RG9_exerciceDejaEnAttente_pasDeSecondRelecteur() {
        exercice.passerEnAttenteDeRelecture();
        presents(auteur, boris, carine);

        service.assigner(exercice);

        verify(relectures, never()).save(any());
        verify(hasard, never()).nextInt(anyInt());
    }

    @Test
    void H1_nouvellePresence_relanceLeTirageDesExercicesDeposes() {
        when(exercices.findBySessionIdAndStatutOrderByDeposeAtAsc(100L, StatutExercice.DEPOSE)).thenReturn(List.of(exercice));
        presents(auteur, boris);
        when(hasard.nextInt(1)).thenReturn(0);

        service.presenceEnregistree(new PresenceEnregistree(new Presence(session, boris, SourcePresence.ETUDIANT, MAINTENANT)));

        assertThat(relecteurAssigne()).isEqualTo(boris);
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.EN_ATTENTE_RELECTURE);
    }

    @Test
    void EF4_depotDExercice_declencheLeTirage() {
        presents(auteur, boris);
        when(hasard.nextInt(1)).thenReturn(0);

        service.exerciceDepose(new ExerciceDepose(exercice));

        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.EN_ATTENTE_RELECTURE);
    }

    @Test
    void EF4_relecture_estDateeAuMomentDuTirage() {
        presents(auteur, boris);
        when(hasard.nextInt(1)).thenReturn(0);

        service.assigner(exercice);

        ArgumentCaptor<Relecture> relecture = ArgumentCaptor.forClass(Relecture.class);
        verify(relectures).save(relecture.capture());
        assertThat(relecture.getValue().getAssigneeAt()).isEqualTo(MAINTENANT);
        assertThat(relecture.getValue().getExercice()).isSameAs(exercice);
    }

    private void presents(Etudiant... etudiants) {
        when(presences.findBySessionId(100L)).thenReturn(java.util.Arrays.stream(etudiants)
                .map(e -> new Presence(session, e, SourcePresence.ETUDIANT, MAINTENANT)).toList());
    }

    private Etudiant relecteurAssigne() {
        ArgumentCaptor<Relecture> relecture = ArgumentCaptor.forClass(Relecture.class);
        verify(relectures).save(relecture.capture());
        return relecture.getValue().getRelecteur();
    }

    private static <T> T avecId(T entite, Long id) {
        ReflectionTestUtils.setField(entite, "id", id);
        return entite;
    }
}
