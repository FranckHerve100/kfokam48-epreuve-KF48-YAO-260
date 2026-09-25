package com.kfokam48.presence.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.kfokam48.presence.domain.PresenceEnregistree;

/**
 * Correctif #50 : le tirage relancé par une présence (H1) ne s'exécute qu'APRÈS la validation de la présence,
 * dans sa propre transaction. Deux pointages simultanés peuvent viser le même exercice : le second tirage
 * échoue alors sur l'unicité de la relecture, sans conséquence (l'exercice a déjà son relecteur) et sans
 * jamais annuler la présence déjà enregistrée.
 */
@Component
public class RelanceTirageApresPresence {

    private static final Logger LOG = LoggerFactory.getLogger(RelanceTirageApresPresence.class);

    private final AssignationService assignation;

    public RelanceTirageApresPresence(AssignationService assignation) {
        this.assignation = assignation;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void apresPresence(PresenceEnregistree evenement) {
        try {
            assignation.presenceEnregistree(evenement);
        } catch (DataAccessException conflit) {
            LOG.info("Tirage relancé par la présence {} abandonné : exercice déjà assigné par un tirage concurrent ({})",
                    evenement.presence().getId(), conflit.getClass().getSimpleName());
        }
    }
}
