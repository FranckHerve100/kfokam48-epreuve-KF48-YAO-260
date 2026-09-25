package com.kfokam48.presence.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "etudiant")
public class Etudiant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "promotion_id", nullable = false)
    private Promotion promotion;

    /** Codes erronés consécutifs (RG4, livré à l'étape 4). */
    @Column(name = "erreurs_code", nullable = false)
    private int erreursCode;

    /** Fin du blocage après 5 codes erronés (RG4, livré à l'étape 4). */
    @Column(name = "bloque_jusqu_a")
    private Instant bloqueJusquA;

    protected Etudiant() {
    }

    public Etudiant(String nom, Promotion promotion) {
        this.nom = nom;
        this.promotion = promotion;
    }

    public boolean appartientA(Promotion autre) {
        return promotion.getId().equals(autre.getId());
    }

    public Long getId() {
        return id;
    }

    public String getNom() {
        return nom;
    }

    public Promotion getPromotion() {
        return promotion;
    }

    public int getErreursCode() {
        return erreursCode;
    }

    public Instant getBloqueJusquA() {
        return bloqueJusquA;
    }
}
