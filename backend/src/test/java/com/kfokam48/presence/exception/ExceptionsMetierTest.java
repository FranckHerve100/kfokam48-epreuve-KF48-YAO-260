package com.kfokam48.presence.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.kfokam48.presence.dto.ErreurDto;

/** Chaque exception métier porte le statut et le code du catalogue de api/contrat.yaml. */
class ExceptionsMetierTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    static Stream<Arguments> catalogue() {
        return Stream.of(
                Arguments.of(PromotionInconnueException.enCorps(), HttpStatus.BAD_REQUEST, "PROMOTION_INCONNUE"),
                Arguments.of(PromotionInconnueException.enChemin(), HttpStatus.NOT_FOUND, "PROMOTION_INCONNUE"),
                Arguments.of(EtudiantInconnuException.enCorps(), HttpStatus.BAD_REQUEST, "ETUDIANT_INCONNU"),
                Arguments.of(EtudiantInconnuException.enChemin(), HttpStatus.NOT_FOUND, "ETUDIANT_INCONNU"),
                Arguments.of(SessionInconnueException.enCorps(), HttpStatus.BAD_REQUEST, "SESSION_INCONNUE"),
                Arguments.of(SessionInconnueException.enChemin(), HttpStatus.NOT_FOUND, "SESSION_INCONNUE"),
                Arguments.of(new CodeInconnuException(), HttpStatus.BAD_REQUEST, "CODE_INCONNU"),
                Arguments.of(new CodeExpireException(), HttpStatus.GONE, "CODE_EXPIRE"),
                Arguments.of(new HorsPromotionException(), HttpStatus.BAD_REQUEST, "HORS_PROMOTION"),
                Arguments.of(new DejaPresentException(), HttpStatus.CONFLICT, "DEJA_PRESENT"),
                Arguments.of(new SessionClotureeException(), HttpStatus.CONFLICT, "SESSION_CLOTUREE"),
                Arguments.of(new LienInvalideException(), HttpStatus.BAD_REQUEST, "LIEN_INVALIDE"),
                Arguments.of(new ExerciceDejaDeposeException(), HttpStatus.CONFLICT, "EXERCICE_DEJA_DEPOSE"),
                Arguments.of(new NoteInvalideException(), HttpStatus.BAD_REQUEST, "NOTE_INVALIDE"),
                Arguments.of(new RelectureInconnueException(), HttpStatus.BAD_REQUEST, "RELECTURE_INCONNUE"),
                Arguments.of(new AutoRelectureException(), HttpStatus.FORBIDDEN, "AUTO_RELECTURE"),
                Arguments.of(new RelecteurNonAssigneException(), HttpStatus.FORBIDDEN, "RELECTEUR_NON_ASSIGNE"),
                Arguments.of(new RelectureDejaRendueException(), HttpStatus.CONFLICT, "RELECTURE_DEJA_RENDUE"));
    }

    @ParameterizedTest(name = "{2} → {1}")
    @MethodSource("catalogue")
    void B4_exceptionMetier_traduiteEnStatutEtCode(ExceptionMetier exception, HttpStatus statut, String code) {
        ResponseEntity<ErreurDto> reponse = handler.metier(exception);

        assertThat(reponse.getStatusCode()).isEqualTo(statut);
        assertThat(reponse.getBody().code()).isEqualTo(code);
        assertThat(reponse.getBody().message()).isNotBlank();
    }
}
