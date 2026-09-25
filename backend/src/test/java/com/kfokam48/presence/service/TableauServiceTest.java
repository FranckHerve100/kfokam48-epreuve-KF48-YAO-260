package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;

import com.kfokam48.presence.dto.LigneTableauDto;
import com.kfokam48.presence.exception.PromotionInconnueException;
import com.kfokam48.presence.repository.EtudiantRepository;
import com.kfokam48.presence.repository.LigneTableau;
import com.kfokam48.presence.repository.PromotionRepository;

class TableauServiceTest {

    private final PromotionRepository promotions = mock(PromotionRepository.class);
    private final EtudiantRepository etudiants = mock(EtudiantRepository.class);
    private final TableauService service = new TableauService(promotions, etudiants);

    @Test
    void RG15_notesRecues12Et16_moyenne14Virgule00() {
        promotion(1L, new LigneTableau(10L, "Abena", 3L, 2L, 14.0, 0L, 0L));

        LigneTableauDto ligne = service.tableau("1").get(0);

        assertThat(ligne.moyenne()).isEqualByComparingTo("14.00").hasScaleOf(2);
        assertThat(ligne).extracting(LigneTableauDto::presences, LigneTableauDto::exercicesDeposes)
                .containsExactly(3, 2);
    }

    @Test
    void RG15_moyenneArrondieADeuxDecimalesAuPlusProche() {
        promotion(1L, new LigneTableau(10L, "Abena", 1L, 1L, 43.0 / 3, 0L, 0L), new LigneTableau(11L, "Boris", 1L, 1L, 14.125, 0L, 0L));

        List<LigneTableauDto> lignes = service.tableau("1");

        assertThat(lignes.get(0).moyenne()).isEqualTo(new BigDecimal("14.33"));
        assertThat(lignes.get(1).moyenne()).isEqualTo(new BigDecimal("14.13"));
    }

    @Test
    void H10_aucuneNoteRecue_moyenneNulle() {
        promotion(1L, new LigneTableau(10L, "Abena", 0L, 0L, null, 0L, 0L));

        assertThat(service.tableau("1").get(0).moyenne()).isNull();
    }

    @Test
    void RG14_relectureNonRendue_compteeEnAttente() {
        promotion(1L, new LigneTableau(10L, "Daniel", 1L, 0L, null, 1L, 0L));

        assertThat(service.tableau("1").get(0).relecturesEnAttente()).isEqualTo(1);
    }

    @Test
    void EF6_promotionInconnue_renvoie404() {
        when(promotions.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> service.tableau("999"))
                .isInstanceOfSatisfying(PromotionInconnueException.class, e -> assertThat(e.getStatut()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "1.5", " "})
    void EF6_promotionIdAbsentOuInvalide_renvoie404PromotionInconnue(String promotionId) {
        assertThatThrownBy(() -> service.tableau(promotionId))
                .isInstanceOfSatisfying(PromotionInconnueException.class, e -> assertThat(e.getStatut()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    private void promotion(Long id, LigneTableau... lignes) {
        when(promotions.existsById(id)).thenReturn(true);
        when(etudiants.tableau(id)).thenReturn(List.of(lignes));
    }

    @Test
    void RG15_uneNoteRetenueProvisoire_moyenneSignaleeProvisoire() {
        promotion(1L, new LigneTableau(10L, "Abena", 1L, 1L, 15.0, 0L, 1L));

        LigneTableauDto ligne = service.tableau("1").get(0);

        assertThat(ligne.moyenne()).isEqualByComparingTo("15.00");
        assertThat(ligne.moyenneProvisoire()).isTrue();
    }

    @Test
    void RG15_notesRetenues14Et15_moyenne14Virgule50NonProvisoire() {
        promotion(1L, new LigneTableau(10L, "Abena", 2L, 2L, 14.5, 0L, 0L));

        LigneTableauDto ligne = service.tableau("1").get(0);

        assertThat(ligne.moyenne()).isEqualTo(new BigDecimal("14.50"));
        assertThat(ligne.moyenneProvisoire()).isFalse();
    }

    @Test
    void H10_aucuneNote_moyenneNulleEtNonProvisoire() {
        promotion(1L, new LigneTableau(10L, "Abena", 1L, 1L, null, 0L, 0L));

        assertThat(service.tableau("1").get(0).moyenneProvisoire()).isFalse();
    }
}
