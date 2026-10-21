package com.example.vieva.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = "com.example.vieva", importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {
    private static final String DOMAIN = "com.example.vieva.domain..";
    private static final String APPLICATION = "com.example.vieva.application..";
    private static final String INPUT_PORTS = "com.example.vieva.application.ports.input..";
    private static final String OUTPUT_PORTS = "com.example.vieva.application.ports.output..";
    private static final String USE_CASES = "com.example.vieva.application.usecases..";
    private static final String ADAPTERS = "com.example.vieva.adapters..";
    private static final String CONTROLLERS = "com.example.vieva.adapters.controllers..";
    private static final String INFRASTRUCTURE = "com.example.vieva.infrastructure..";

    // R1: Domain does not depend on Application, Adapters, or Infrastructure
    @ArchTest
    public static final ArchRule domainDoesNotDependOnOuterLayers = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(APPLICATION, ADAPTERS, INFRASTRUCTURE);

    // R2: Domain has zero dependencies on frameworks (Spring, JPA, Hibernate, Jackson, etc.)
    @ArchTest
    public static final ArchRule domainHasZeroDependenciesOnFrameworks = noClasses()
            .that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "org.hibernate..",
                    "com.fasterxml.jackson..",
                    "jakarta.validation..",
                    "jakarta.servlet..",
                    "jakarta.transaction.."
            );

    // R3: Application does not depend on Adapters, Infrastructure, or frameworks (Spring, Jakarta, Hibernate, Jackson)
    @ArchTest
    public static final ArchRule applicationDoesNotDependOnAdaptersInfraOrSpring = noClasses()
            .that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    ADAPTERS,
                    INFRASTRUCTURE,
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.validation..",
                    "jakarta.servlet..",
                    "jakarta.transaction..",
                    "org.hibernate..",
                    "com.fasterxml.jackson.."
            );

    // R4: Output ports do not depend on Input ports
    @ArchTest
    public static final ArchRule outputPortsDoNotDependOnInputPorts = noClasses()
            .that().resideInAPackage(OUTPUT_PORTS)
            .should().dependOnClassesThat().resideInAPackage(INPUT_PORTS);

    // R4b: Input ports do not depend on Output ports
    @ArchTest
    public static final ArchRule inputPortsDoNotDependOnOutputPorts = noClasses()
            .that().resideInAPackage(INPUT_PORTS)
            .should().dependOnClassesThat().resideInAPackage(OUTPUT_PORTS);

    // R4c: Ports (Input & Output) do not depend on UseCase implementations
    @ArchTest
    public static final ArchRule portsDoNotDependOnUseCaseImplementations = noClasses()
            .that().resideInAnyPackage(INPUT_PORTS, OUTPUT_PORTS)
            .should().dependOnClassesThat().resideInAPackage(USE_CASES);

    // R5: Adapters do not depend on Infrastructure
    @ArchTest
    public static final ArchRule adaptersDoNotDependOnInfrastructure = noClasses()
            .that().resideInAPackage(ADAPTERS)
            .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE);

    // R6: Input ports do not depend on Domain entities
    @ArchTest
    public static final ArchRule inputPortsDoNotDependOnDomainEntities = noClasses()
            .that().resideInAPackage(INPUT_PORTS)
            .should().dependOnClassesThat().resideInAPackage("com.example.vieva.domain.entities..");

    // R7: Controllers do not depend on UseCase implementations (only on Input ports)
    @ArchTest
    public static final ArchRule controllersDoNotDependOnUseCaseImplementations = noClasses()
            .that().resideInAPackage(CONTROLLERS)
            .should().dependOnClassesThat().resideInAPackage(USE_CASES);

    // R8: Adapters do not depend on Domain entities
    @ArchTest
    public static final ArchRule adaptersDoNotDependOnDomainEntities = noClasses()
            .that().resideInAPackage(ADAPTERS)
            .should().dependOnClassesThat().resideInAPackage("com.example.vieva.domain.entities..");

    // R9: Presenters do not depend on Domain entities
    @ArchTest
    public static final ArchRule presentersDoNotDependOnDomainEntities = noClasses()
            .that().resideInAPackage("com.example.vieva.adapters.presenters..")
            .should().dependOnClassesThat().resideInAPackage("com.example.vieva.domain.entities..");

    // R10: Controllers do not depend on Domain entities
    @ArchTest
    public static final ArchRule controllersDoNotDependOnDomainEntities = noClasses()
            .that().resideInAPackage(CONTROLLERS)
            .should().dependOnClassesThat().resideInAPackage("com.example.vieva.domain.entities..");

    // R11: Controllers do not depend on Output ports (only on Input ports)
    @ArchTest
    public static final ArchRule controllersDoNotDependOnOutputPorts = noClasses()
            .that().resideInAPackage(CONTROLLERS)
            .should().dependOnClassesThat().resideInAPackage(OUTPUT_PORTS);

    // R12: Adapters do not depend on Output ports
    @ArchTest
    public static final ArchRule adaptersDoNotDependOnOutputPorts = noClasses()
            .that().resideInAPackage(ADAPTERS)
            .should().dependOnClassesThat().resideInAPackage(OUTPUT_PORTS);

    // R13: Infrastructure does not depend on Adapters
    @ArchTest
    public static final ArchRule infrastructureDoesNotDependOnAdapters = noClasses()
            .that().resideInAPackage(INFRASTRUCTURE)
            .should().dependOnClassesThat().resideInAPackage(ADAPTERS);

    // R14: Infrastructure database repositories do not depend on Input ports
    @ArchTest
    public static final ArchRule infrastructureDatabaseDoesNotDependOnInputPorts = noClasses()
            .that().resideInAPackage("com.example.vieva.infrastructure.database..")
            .should().dependOnClassesThat().resideInAPackage(INPUT_PORTS);
}
