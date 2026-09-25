package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import com.kfokam48.presence.domain.Etudiant;
import com.kfokam48.presence.domain.Exercice;
import com.kfokam48.presence.domain.Promotion;
import com.kfokam48.presence.domain.Relecture;
import com.kfokam48.presence.domain.Session;
import com.kfokam48.presence.domain.StatutExercice;
import com.kfokam48.presence.dto.RelectureAssigneeDto;
import com.kfokam48.presence.dto.RelectureDemande;
import com.kfokam48.presence.dto.StatutRelecture;
import com.kfokam48.presence.exception.AutoRelectureException;
import com.kfokam48.presence.exception.EtudiantInconnuException;
import com.kfokam48.presence.exception.RelecteurNonAssigneException;
import com.kfokam48.presence.exception.RelectureDejaRendueException;
import com.kfokam48.presence.exception.RelectureInconnueException;
import com.kfokam48.presence.repository.EtudiantRepository;
import com.kfokam48.presence.repository.RelectureRepository;

class RelectureServiceTest {

    private static final Instant MAINTENANT = Instant.parse("2026-09-25T11:00:00Z");

    private final RelectureRepository relectures = mock(RelectureRepository.class);
    private final EtudiantRepository etudiants = mock(EtudiantRepository.class);
    private final RelectureService service = new RelectureService(relectures, etudiants, Clock.fixed(MAINTENANT, ZoneOffset.UTC));

    private final Promotion promotion = new Promotion("KF48 Yaoundé");
    private final Session session = new Session("Cours", promotion, "AB23CD", MAINTENANT.minusSeconds(3600));
    private final Etudiant auteur = avecId(new Etudiant("Abena", promotion), 10L);
    private final Etudiant relecteur = avecId(new Etudiant("Boris", promotion), 11L);
    private Exercice exercice;
    private Relecture relecture;

    @BeforeEach
    void preparer() {
        exercice = avecId(new Exercice(session, auteur, "https://github.com/abena/ex", MAINTENANT), 500L);
        exercice.passerEnAttenteDeRelecture();
        relecture = avecId(new Relecture(exercice, relecteur, MAINTENANT), 700L);
        when(relectures.findById(700L)).thenReturn(Optional.of(relecture));
    }

    @Test
    void EF5_relecteurAssigne_noteEnregistreeEtExerciceRelu() {
        service.rendre(700L, new RelectureDemande(14, "Bien structuré"), 11L);

        assertThat(relecture.getNote()).isEqualTo(14);
        assertThat(relecture.getCommentaire()).isEqualTo("Bien structuré");
        assertThat(relecture.getRendueAt()).isEqualTo(MAINTENANT);
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.RELU);
    }

    @Test
    void H11_sansEnTete_pasDeControleDIdentite_noteEnregistree() {
        service.rendre(700L, new RelectureDemande(0, "À reprendre"), null);

        assertThat(relecture.estRendue()).isTrue();
        assertThat(exercice.getStatut()).isEqualTo(StatutExercice.RELU);
    }

    @Test
    void RG10_auteurRelitSonPropreExercice_renvoie403AutoRelecture() {
        RelectureDemande demande = new RelectureDemande(20, "Parfait");

        assertThatThrownBy(() -> service.rendre(700L, demande, 10L)).isInstanceOf(AutoRelectureException.class);
        assertThat(relecture.estRendue()).isFalse();
    }

    @Test
    void H11_etudiantNonAssigne_renvoie403RelecteurNonAssigne() {
        RelectureDemande demande = new RelectureDemande(12, "Correct");

        assertThatThrownBy(() -> service.rendre(700L, demande, 99L)).isInstanceOf(RelecteurNonAssigneException.class);
        assertThat(relecture.estRendue()).isFalse();
    }

    @Test
    void RG12_secondEnvoi_renvoie409EtNoteInchangee() {
        service.rendre(700L, new RelectureDemande(14, "Première note"), 11L);
        RelectureDemande seconde = new RelectureDemande(18, "Je change d'avis");

        assertThatThrownBy(() -> service.rendre(700L, seconde, 11L)).isInstanceOf(RelectureDejaRendueException.class);
        assertThat(relecture.getNote()).isEqualTo(14);
    }

    @Test
    void EF5_relectureInconnue_renvoie400RelectureInconnue() {
        when(relectures.findById(999L)).thenReturn(Optional.empty());
        RelectureDemande demande = new RelectureDemande(14, "Bien");

        assertThatThrownBy(() -> service.rendre(999L, demande, 11L)).isInstanceOf(RelectureInconnueException.class);
    }

    @Test
    void EF5_relecturesDUnEtudiant_enAttenteDabord() {
        Relecture rendue = avecId(new Relecture(exercice, relecteur, MAINTENANT.minusSeconds(7200)), 701L);
        rendue.rendre(15, "Déjà fait", MAINTENANT.minusSeconds(3600));
        when(etudiants.existsById(11L)).thenReturn(true);
        when(relectures.findByRelecteurIdOrderByAssigneeAtDesc(11L)).thenReturn(List.of(rendue, relecture));

        List<RelectureAssigneeDto> liste = service.relecturesDe(11L);

        assertThat(liste).extracting(RelectureAssigneeDto::statut).containsExactly(StatutRelecture.EN_ATTENTE, StatutRelecture.RENDUE);
        assertThat(liste.get(0)).isEqualTo(new RelectureAssigneeDto(700L, 500L, "https://github.com/abena/ex", StatutRelecture.EN_ATTENTE));
    }

    @Test
    void EF5_relecturesDUnEtudiantInconnu_renvoie404() {
        when(etudiants.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.relecturesDe(99L))
                .isInstanceOfSatisfying(EtudiantInconnuException.class, e -> assertThat(e.getStatut()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private static <T> T avecId(T entite, Long id) {
        ReflectionTestUtils.setField(entite, "id", id);
        return entite;
    }
}
