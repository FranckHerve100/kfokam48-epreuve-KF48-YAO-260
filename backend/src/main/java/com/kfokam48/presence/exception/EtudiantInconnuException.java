package com.kfokam48.presence.exception;

import org.springframework.http.HttpStatus;

/** 400 quand l'identifiant vient du corps de la requête, 404 quand il vient du chemin. */
public class EtudiantInconnuException extends ExceptionMetier {

    private EtudiantInconnuException(HttpStatus statut) {
        super(statut, "ETUDIANT_INCONNU", "Cet étudiant n'existe pas.");
    }

    public static EtudiantInconnuException enCorps() {
        return new EtudiantInconnuException(HttpStatus.BAD_REQUEST);
    }

    public static EtudiantInconnuException enChemin() {
        return new EtudiantInconnuException(HttpStatus.NOT_FOUND);
    }
}
