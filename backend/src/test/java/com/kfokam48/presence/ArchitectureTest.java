package com.kfokam48.presence;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import jakarta.persistence.Entity;

/** Séparation des couches imposée par B3 (ARCHITECTURE.md §3). */
@AnalyzeClasses(packages = "com.kfokam48.presence", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule B3_controleur_nAppelleJamaisUnRepository = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().resideInAPackage("..repository..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule B3_controleur_nExposeJamaisUneEntite = noClasses()
            .that().resideInAPackage("..controller..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule B3_dto_neDependPasDuDomaine = noClasses()
            .that().resideInAPackage("..dto..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule B3_couches_respectentLeSensDesDependances = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .withOptionalLayers(true)
            .layer("Controller").definedBy("..controller..")
            .layer("Service").definedBy("..service..")
            .layer("Repository").definedBy("..repository..")
            .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
            .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller")
            .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");
}
