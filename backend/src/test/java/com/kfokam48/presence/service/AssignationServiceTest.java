package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

/**
 * Tirage des relecteurs, v2 (#52) : deux relecteurs différents par exercice (RG9), parmi les présents sauf
 * l'auteur, les moins chargés d'abord puis au hasard (RG10) ; les relecteurs manquants sont tirés aux
 * présences suivantes (H1).
 */
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
    private final Etudiant daniel = avecId(new Etudiant("Daniel", promotion), 13L);
    private Exercice exercice;

    @BeforeEach
    void preparer() {
        exercice = avecId(new Exercice(session, auteur, "https://github.com/abena/ex", MAINTENANT), 500L);
        when(relectures.save(any(Relecture.class))).thenAnswer(appel -> appel.getArgument(0));
        when(relectures.findByExerciceId(500L)).thenReturn(List.of());
        when(hasard.nextInt(anyInt())).thenReturn(0);
    }

    @Test
    void RG9_troisPresentsDontLAuteur_lesDeuxAutresSontAssignes() {
        presents(auteur, boris, carine);

        service.assigner(exercice);

        assertThat(relecteursAssignes()).containsExactlyInAnyOrder(boris, carine);
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.EN_ATTENTE_RELECTURE);
    }

    @Test
    void RG9_quatrePresents_deuxRelecteursSeulement() {
        presents(auteur, boris, carine, daniel);

        service.assigner(exercice);

        assertThat(relecteursAssignes()).hasSize(2).doesNotContain(auteur);
    }

    @Test
    void RG10_jamaisLAuteur_seulCandidatAssigneEnAttendantLeSecond() {
        presents(auteur, boris);

        service.assigner(exercice);

        assertThat(relecteursAssignes()).containsExactly(boris);
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.EN_ATTENTE_RELECTURE);
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
    void RG10_lesMoinsChargesSontChoisisEnPremier() {
        presents(auteur, boris, carine, daniel);
        when(relectures.countByRelecteurIdAndExerciceSessionId(11L, 100L)).thenReturn(3L);
        when(relectures.countByRelecteurIdAndExerciceSessionId(12L, 100L)).thenReturn(0L);
        when(relectures.countByRelecteurIdAndExerciceSessionId(13L, 100L)).thenReturn(1L);

        service.assigner(exercice);

        assertThat(relecteursAssignes()).containsExactly(carine, daniel);
    }

    @Test
    void RG10_exAequo_departageAuHasard() {
        presents(auteur, boris, carine, daniel);
        when(relectures.countByRelecteurIdAndExerciceSessionId(anyLong(), anyLong())).thenReturn(1L);
        when(hasard.nextInt(3)).thenReturn(2);
        when(hasard.nextInt(2)).thenReturn(0);

        service.assigner(exercice);

        assertThat(relecteursAssignes()).containsExactly(daniel, boris);
    }

    @Test
    void RG9_secondRelecteurManquant_tireSansReprendreLePremier() {
        exercice.passerEnAttenteDeRelecture();
        when(relectures.findByExerciceId(500L)).thenReturn(List.of(new Relecture(exercice, boris, MAINTENANT)));
        presents(auteur, boris, carine);

        service.assigner(exercice);

        assertThat(relecteursAssignes()).containsExactly(carine);
    }

    @Test
    void RG9_deuxRelecteursDejaAssignes_pasDeTroisieme() {
        exercice.passerEnAttenteDeRelecture();
        when(relectures.findByExerciceId(500L)).thenReturn(List.of(
                new Relecture(exercice, boris, MAINTENANT), new Relecture(exercice, carine, MAINTENANT)));
        presents(auteur, boris, carine, daniel);

        service.assigner(exercice);

        verify(relectures, never()).save(any());
    }

    @Test
    void RG9_exerciceRelu_pasDeNouveauRelecteur() {
        exercice.marquerRelu();
        presents(auteur, boris, carine);

        service.assigner(exercice);

        verify(relectures, never()).save(any());
    }

    @Test
    void H1_nouvellePresence_relanceLeTirageDesExercicesIncomplets() {
        when(exercices.findBySessionIdAndStatutInOrderByDeposeAtAsc(100L, AssignationService.STATUTS_A_COMPLETER))
                .thenReturn(List.of(exercice));
        presents(auteur, boris);

        service.presenceEnregistree(new PresenceEnregistree(new Presence(session, boris, SourcePresence.ETUDIANT, MAINTENANT)));

        assertThat(relecteursAssignes()).containsExactly(boris);
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.EN_ATTENTE_RELECTURE);
    }

    @Test
    void EF4_depotDExercice_declencheLeTirage() {
        presents(auteur, boris);

        service.exerciceDepose(new ExerciceDepose(exercice));

        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.EN_ATTENTE_RELECTURE);
    }

    @Test
    void EF4_relecture_estDateeAuMomentDuTirage() {
        presents(auteur, boris);

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

    private List<Etudiant> relecteursAssignes() {
        ArgumentCaptor<Relecture> relecture = ArgumentCaptor.forClass(Relecture.class);
        verify(relectures, org.mockito.Mockito.atLeastOnce()).save(relecture.capture());
        return relecture.getAllValues().stream().map(Relecture::getRelecteur).toList();
    }

    private static <T> T avecId(T entite, Long id) {
        ReflectionTestUtils.setField(entite, "id", id);
        return entite;
    }
}
