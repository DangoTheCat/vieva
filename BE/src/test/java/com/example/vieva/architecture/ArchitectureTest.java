package com.example.vieva.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@AnalyzeClasses(packages = "com.example.vieva", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {
    private static final String DOMAIN = "com.example.vieva.domain..";
    private static final String APPLICATION = "com.example.vieva.application..";
    private static final String OUTPUT_PORTS = "com.example.vieva.application.ports.output..";
    private static final String USE_CASES = "com.example.vieva.application.usecases..";
    private static final String ADAPTERS = "com.example.vieva.adapters..";
    private static final String INFRA = "com.example.vieva.infrastructure..";
    private static final String MAIN = "com.example.vieva.infrastructure.configuration..";
    private static final String GATEWAYS_DB = "com.example.vieva.infrastructure.database..";

    // R1: domain không biết vòng ngoài
    @ArchTest
    static final ArchRule domainIsPure = noClasses().that().resideInAPackage(DOMAIN)
            .should().dependOnClassesThat().resideInAnyPackage(
                    APPLICATION, ADAPTERS, INFRA, "org.springframework..", "jakarta.persistence..");

    // R2: application không biết framework và vòng ngoài
    @ArchTest
    static final ArchRule applicationIsFrameworkFree = noClasses().that().resideInAPackage(APPLICATION)
            .should().dependOnClassesThat().resideInAnyPackage(
                    ADAPTERS, INFRA, "org.springframework..", "jakarta.persistence..");

    // R3: adapters chỉ đi qua input port, không nhảy bậc
    @ArchTest
    static final ArchRule adaptersUseInputPortsOnly = FreezingArchRule.freeze(noClasses().that().resideInAPackage(ADAPTERS)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN, INFRA, OUTPUT_PORTS, USE_CASES));

    // R4: infrastructure không gọi interactor cụ thể, không gọi adapters
    @ArchTest
    static final ArchRule infraDoesNotCallInteractors = FreezingArchRule.freeze(noClasses().that().resideInAPackage(INFRA)
            .and().resideOutsideOfPackage(MAIN)
            .should().dependOnClassesThat().resideInAnyPackage(USE_CASES, ADAPTERS));

    // R5: ngoài gateway database và Main, infrastructure không chạm domain
    @ArchTest
    static final ArchRule onlyDbGatewaysTouchDomain = FreezingArchRule.freeze(noClasses().that().resideInAPackage(INFRA)
            .and().resideOutsideOfPackages(MAIN, GATEWAYS_DB)
            .should().dependOnClassesThat().resideInAnyPackage(DOMAIN));

    // R6: entity không có setter public
    @ArchTest
    static final ArchRule entitiesHaveNoSetters = FreezingArchRule.freeze(noMethods()
            .that().arePublic().and().haveNameMatching("set[A-Z].*")
            .should().beDeclaredInClassesThat().resideInAPackage("com.example.vieva.domain.entities.."));

    // R7: adapters không phụ thuộc vào usecases cụ thể
    @ArchTest
    static final ArchRule adaptersDoNotDependOnUseCases = noClasses().that().resideInAPackage(ADAPTERS)
            .should().dependOnClassesThat().resideInAnyPackage(USE_CASES);

    // R8: adapters không phụ thuộc vào *Repository hoặc *Port trong output ports
    @ArchTest
    static final ArchRule adaptersDoNotDependOnOutputRepositoriesOrPorts = noClasses().that().resideInAPackage(ADAPTERS)
            .should().dependOnClassesThat(
                    new com.tngtech.archunit.base.DescribedPredicate<>("are output repositories or ports") {
                        @Override
                        public boolean test(com.tngtech.archunit.core.domain.JavaClass javaClass) {
                            return javaClass.getPackageName().startsWith("com.example.vieva.application.ports.output")
                                    && (javaClass.getSimpleName().endsWith("Repository") || javaClass.getSimpleName().endsWith("Port"));
                        }
                    });
}
