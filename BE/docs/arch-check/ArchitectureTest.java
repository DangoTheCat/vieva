package com.example.vieva.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchIgnore;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(packages = "com.example.vieva", importOptions = ImportOption.DoNotIncludeTests.class)
/**
 * Clean Architecture rules (restored from commit ed43893, removed in 16d95fd) plus rules for the
 * violations found in the 31/10/2026 audit. Run: mvn test -Dtest=ArchitectureTest
 */
public class ArchitectureTest {
    private static final String DOMAIN = "com.example.vieva.domain..";
    private static final String APPLICATION = "com.example.vieva.application..";
    private static final String INPUT_PORTS = "com.example.vieva.application.ports.input..";
    private static final String OUTPUT_PORTS = "com.example.vieva.application.ports.output..";
    private static final String USE_CASES = "com.example.vieva.application.usecases..";
    private static final String ADAPTERS = "com.example.vieva.adapters..";
    private static final String CONTROLLERS = "com.example.vieva.adapters.controllers..";
    private static final String INFRASTRUCTURE = "com.example.vieva.infrastructure..";

    @ArchTest
    public static final ArchRule domainDoesNotDependOnOuterLayers = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, ADAPTERS, INFRASTRUCTURE);

    @ArchTest
    public static final ArchRule domainHasZeroDependenciesOnFrameworks = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "org.hibernate..",
                    "com.fasterxml.jackson..", "jakarta.validation..", "jakarta.servlet..", "jakarta.transaction..");

    @ArchTest
    public static final ArchRule applicationDoesNotDependOnAdaptersInfraOrSpring = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    ADAPTERS, INFRASTRUCTURE, "org.springframework..", "jakarta.persistence..",
                    "jakarta.validation..", "jakarta.servlet..", "jakarta.transaction..",
                    "org.hibernate..", "com.fasterxml.jackson..");

    @ArchTest
    public static final ArchRule outputPortsDoNotDependOnInputPorts = noClasses()
            .that().resideInAPackage(OUTPUT_PORTS)
            .should().dependOnClassesThat().resideInAPackage(INPUT_PORTS);

    @ArchTest
    public static final ArchRule inputPortsDoNotDependOnOutputPorts = noClasses()
            .that().resideInAPackage(INPUT_PORTS)
            .should().dependOnClassesThat().resideInAPackage(OUTPUT_PORTS);

    @ArchTest
    public static final ArchRule portsDoNotDependOnUseCaseImplementations = noClasses()
            .that().resideInAnyPackage(INPUT_PORTS, OUTPUT_PORTS)
            .should().dependOnClassesThat().resideInAPackage(USE_CASES);

    @ArchTest
    public static final ArchRule adaptersDoNotDependOnInfrastructure = noClasses()
            .that().resideInAPackage(ADAPTERS)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE);

    @ArchTest
    public static final ArchRule inputPortsDoNotDependOnDomainEntities = noClasses()
            .that().resideInAPackage(INPUT_PORTS)
            .should().dependOnClassesThat().resideInAPackage("com.example.vieva.domain.entities..");

    @ArchTest
    public static final ArchRule controllersDoNotDependOnUseCaseImplementations = noClasses()
            .that().resideInAPackage(CONTROLLERS)
            .should().dependOnClassesThat().resideInAPackage(USE_CASES);

    @ArchTest
    public static final ArchRule adaptersDoNotDependOnDomainEntities = noClasses()
            .that().resideInAPackage(ADAPTERS)
            .should().dependOnClassesThat().resideInAPackage("com.example.vieva.domain.entities..");

    @ArchTest
    public static final ArchRule presentersDoNotDependOnDomainEntities = noClasses()
            .that().resideInAPackage("com.example.vieva.adapters.presenters..")
            .should().dependOnClassesThat().resideInAPackage("com.example.vieva.domain.entities..");

    @ArchTest
    public static final ArchRule controllersDoNotDependOnDomainEntities = noClasses()
            .that().resideInAPackage(CONTROLLERS)
            .should().dependOnClassesThat().resideInAPackage("com.example.vieva.domain.entities..");

    @ArchTest
    public static final ArchRule controllersDoNotDependOnOutputPorts = noClasses()
            .that().resideInAPackage(CONTROLLERS)
            .should().dependOnClassesThat().resideInAPackage(OUTPUT_PORTS);

    @ArchTest
    public static final ArchRule adaptersDoNotDependOnOutputPorts = noClasses()
            .that().resideInAPackage(ADAPTERS)
            .should().dependOnClassesThat().resideInAPackage(OUTPUT_PORTS);

    @ArchTest
    public static final ArchRule infrastructureDoesNotDependOnAdapters = noClasses()
            .that().resideInAPackage(INFRASTRUCTURE)
            .should().dependOnClassesThat().resideInAPackage(ADAPTERS);

    @ArchTest
    public static final ArchRule infrastructureDatabaseDoesNotDependOnInputPorts = noClasses()
            .that().resideInAPackage("com.example.vieva.infrastructure.database..")
            .should().dependOnClassesThat().resideInAPackage(INPUT_PORTS);

    // ---- Rules added on 31/10/2026 (audit A2, A3, A4) ----

    private static final DescribedPredicate<JavaClass> INTERVIEW_ENTITIES = DescribedPredicate.describe(
            "interview domain entities (ExamAttempt, ExamTurn, Rubric, RubricCriterion)",
            c -> c.isEquivalentTo(com.example.vieva.domain.entities.ExamAttempt.class)
                    || c.isEquivalentTo(com.example.vieva.domain.entities.ExamTurn.class)
                    || c.isEquivalentTo(com.example.vieva.domain.entities.Rubric.class)
                    || c.isEquivalentTo(com.example.vieva.domain.entities.RubricCriterion.class));

    private static final DescribedPredicate<JavaClass> AI_GRADING_PORTS = DescribedPredicate.describe(
            "AI grading output ports",
            c -> c.getPackageName().startsWith("com.example.vieva.application.ports.output")
                    && (c.getSimpleName().endsWith("Gateway") || c.getSimpleName().endsWith("GraderPort")
                    || c.getSimpleName().equals("AttemptGradingResult")));

    // X1: AI grading ports and adapters exchange plain DTOs (EvaluationTurn, RubricSummary), not entities
    @ArchTest
    public static final ArchRule aiGradingPortsDoNotExposeInterviewEntities = noClasses()
            .that(AI_GRADING_PORTS)
            .should().dependOnClassesThat(INTERVIEW_ENTITIES);

    @ArchTest
    public static final ArchRule aiAdaptersDoNotDependOnInterviewEntities = noClasses()
            .that().resideInAPackage("com.example.vieva.infrastructure.ai..")
            .should().dependOnClassesThat(INTERVIEW_ENTITIES);

    // X2: infrastructure reaches the application only through ports (configuration wires use cases)
    @ArchTest
    public static final ArchRule infrastructureDoesNotDependOnUseCases = noClasses()
            .that().resideInAPackage(INFRASTRUCTURE)
            .and().resideOutsideOfPackage("com.example.vieva.infrastructure.configuration..")
            .should().dependOnClassesThat().resideInAPackage(USE_CASES);

    // X3: entities protect their invariants — state changes go through business methods
    @ArchIgnore(reason = "Plan Đợt 3 (task 3.1): remove @Setter from the 23 entities, then enable")
    @ArchTest
    public static final ArchRule entitiesHaveNoPublicSetters = noMethods()
            .that().arePublic().and().haveNameMatching("set[A-Z].*")
            .should().beDeclaredInClassesThat().resideInAPackage("com.example.vieva.domain.entities..");
}
