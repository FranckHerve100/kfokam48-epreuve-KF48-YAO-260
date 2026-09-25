package com.kfokam48.presence.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.kfokam48.presence.domain.Etudiant;
import com.kfokam48.presence.domain.Promotion;
import com.kfokam48.presence.domain.Session;
import com.kfokam48.presence.domain.StatutExercice;
import com.kfokam48.presence.dto.MarquagePresenceDemande;
import com.kfokam48.presence.repository.ExerciceRepository;
import com.kfokam48.presence.repository.PresenceRepository;
import com.kfokam48.test.JeuDEssai;

/**
 * Non-régression #50 : « deux étudiants côte à côte tapent le code presque en même temps, un seul apparaît ».
 * Transactions réelles (pas de @Transactional sur le test) : la première pointe et garde sa transaction ouverte
 * pendant que la seconde pointe à son tour, ce qui reproduit de façon déterministe le « presque en même temps ».
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(JeuDEssai.class)
class PointageSimultaneIT {

    private static final String CODE = "NR5A2B";

    @Autowired
    private PresenceService presenceService;

    @Autowired
    private PresenceRepository presences;

    @Autowired
    private ExerciceRepository exercices;

    @Autowired
    private PlatformTransactionManager transactions;

    @Autowired
    private JeuDEssai jeu;

    private final ExecutorService fils = Executors.newFixedThreadPool(2);

    @AfterEach
    void arreter() {
        fils.shutdownNow();
    }

    @Test
    void NR_50_deuxPointagesSimultanes_lesDeuxPresencesSontEnregistrees() throws Exception {
        // Given : une session ouverte dont un exercice attend encore son relecteur (auteur seul présent, H1)
        Promotion promotion = jeu.promotion();
        Etudiant auteur = jeu.etudiant(promotion);
        Etudiant premier = jeu.etudiant(promotion);
        Etudiant second = jeu.etudiant(promotion);
        Session session = jeu.sessionOuverteIlYA(promotion, CODE, 1);
        jeu.presence(session, auteur);
        Long exerciceId = jeu.exercice(session, auteur).getId();
        assertThat(exercices.findById(exerciceId)).get().extracting("statut").isEqualTo(StatutExercice.DEPOSE);

        // When : le premier pointe, sa transaction n'est pas encore validée quand le second pointe
        CountDownLatch premierAPointe = new CountDownLatch(1);
        CountDownLatch secondATermine = new CountDownLatch(1);
        Future<?> pointagePremier = fils.submit(() -> new TransactionTemplate(transactions).executeWithoutResult(etat -> {
            presenceService.marquer(new MarquagePresenceDemande(CODE, premier.getId()));
            premierAPointe.countDown();
            attendre(secondATermine);
        }));
        assertThat(premierAPointe.await(10, TimeUnit.SECONDS)).isTrue();
        Future<?> pointageSecond = fils.submit(() -> presenceService.marquer(new MarquagePresenceDemande(CODE, second.getId())));
        String erreurSecond = issue(pointageSecond);
        secondATermine.countDown();
        String erreurPremier = issue(pointagePremier);

        // Then : les deux présences sont enregistrées
        assertThat(erreurPremier).as("pointage du premier étudiant").isNull();
        assertThat(erreurSecond).as("pointage du second étudiant").isNull();
        assertThat(presences.existsBySessionIdAndEtudiantId(session.getId(), premier.getId())).isTrue();
        assertThat(presences.existsBySessionIdAndEtudiantId(session.getId(), second.getId())).isTrue();
    }

    /** null si le pointage a réussi, sinon la cause de l'échec. */
    private static String issue(Future<?> pointage) throws InterruptedException {
        try {
            pointage.get(30, TimeUnit.SECONDS);
            return null;
        } catch (ExecutionException echec) {
            return echec.getCause().getClass().getSimpleName() + " : " + echec.getCause().getMessage();
        } catch (java.util.concurrent.TimeoutException bloque) {
            return "pointage bloqué plus de 30 s";
        }
    }

    private static void attendre(CountDownLatch signal) {
        try {
            signal.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException interrompu) {
            Thread.currentThread().interrupt();
        }
    }
}
