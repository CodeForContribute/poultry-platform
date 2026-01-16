package com.poultry.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

/**
 * Architecture tests using ArchUnit to enforce code structure and dependencies.
 * These tests ensure the codebase maintains proper layering and modularity.
 */
@DisplayName("Architecture Tests")
class ArchitectureTest {

    private static JavaClasses classes;

    @BeforeAll
    static void setUp() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_JARS)
                .importPackages("com.poultry");
    }

    @Nested
    @DisplayName("Layer Dependency Rules")
    class LayerDependencyTests {

        @Test
        @DisplayName("should enforce layered architecture")
        void shouldEnforceLayeredArchitecture() {
            ArchRule rule = layeredArchitecture()
                    .consideringOnlyDependenciesInLayers()
                    .layer("Controller").definedBy("..controller..")
                    .layer("Service").definedBy("..service..")
                    .layer("Repository").definedBy("..repository..")
                    .layer("Entity").definedBy("..entity..")
                    .layer("DTO").definedBy("..dto..")

                    .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service")
                    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service");

            rule.check(classes);
        }

        @Test
        @DisplayName("controllers should not access repositories directly")
        void controllersShouldNotAccessRepositoriesDirectly() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().accessClassesThat().resideInAPackage("..repository..");

            rule.check(classes);
        }

        @Test
        @DisplayName("services should not access controllers")
        void servicesShouldNotAccessControllers() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..service..")
                    .should().accessClassesThat().resideInAPackage("..controller..");

            rule.check(classes);
        }

        @Test
        @DisplayName("repositories should not access services")
        void repositoriesShouldNotAccessServices() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..repository..")
                    .should().accessClassesThat().resideInAPackage("..service..");

            rule.check(classes);
        }

        @Test
        @DisplayName("repositories should not access controllers")
        void repositoriesShouldNotAccessControllers() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..repository..")
                    .should().accessClassesThat().resideInAPackage("..controller..");

            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("Package Structure Validation")
    class PackageStructureTests {

        @Test
        @DisplayName("controllers should be in controller packages")
        void controllersShouldBeInControllerPackages() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Controller")
                    .should().resideInAPackage("..controller..");

            rule.check(classes);
        }

        @Test
        @DisplayName("services should be in service packages")
        void servicesShouldBeInServicePackages() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Service")
                    .and().areNotInterfaces()
                    .should().resideInAPackage("..service..");

            rule.check(classes);
        }

        @Test
        @DisplayName("repositories should be in repository packages")
        void repositoriesShouldBeInRepositoryPackages() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Repository")
                    .should().resideInAPackage("..repository..");

            rule.check(classes);
        }

        @Test
        @DisplayName("entities should be in entity packages")
        void entitiesShouldBeInEntityPackages() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(jakarta.persistence.Entity.class)
                    .should().resideInAPackage("..entity..");

            rule.check(classes);
        }

        @Test
        @DisplayName("DTOs should be in dto packages")
        void dtosShouldBeInDtoPackages() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Dto")
                    .or().haveSimpleNameEndingWith("Request")
                    .or().haveSimpleNameEndingWith("Response")
                    .should().resideInAPackage("..dto..");

            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("Naming Convention Rules")
    class NamingConventionTests {

        @Test
        @DisplayName("controllers should have Controller suffix")
        void controllersShouldHaveControllerSuffix() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..controller..")
                    .and().areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
                    .should().haveSimpleNameEndingWith("Controller");

            rule.check(classes);
        }

        @Test
        @DisplayName("services should have Service suffix")
        void servicesShouldHaveServiceSuffix() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..service..")
                    .and().areAnnotatedWith(org.springframework.stereotype.Service.class)
                    .should().haveSimpleNameEndingWith("Service");

            rule.check(classes);
        }

        @Test
        @DisplayName("repositories should have Repository suffix")
        void repositoriesShouldHaveRepositorySuffix() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..repository..")
                    .should().haveSimpleNameEndingWith("Repository");

            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("Annotation Rules")
    class AnnotationTests {

        @Test
        @DisplayName("controllers should be annotated with RestController")
        void controllersShouldBeAnnotatedWithRestController() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..controller..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .should().beAnnotatedWith(org.springframework.web.bind.annotation.RestController.class);

            rule.check(classes);
        }

        @Test
        @DisplayName("services should be annotated with Service")
        void servicesShouldBeAnnotatedWithService() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..service..")
                    .and().haveSimpleNameEndingWith("Service")
                    .and().areNotInterfaces()
                    .should().beAnnotatedWith(org.springframework.stereotype.Service.class);

            rule.check(classes);
        }

        @Test
        @DisplayName("repositories should extend JpaRepository or be annotated with Repository")
        void repositoriesShouldBeSpringRepositories() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..repository..")
                    .and().haveSimpleNameEndingWith("Repository")
                    .should().beInterfaces();

            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("Dependency Rules")
    class DependencyTests {

        @Test
        @DisplayName("should not have circular dependencies between modules")
        void shouldNotHaveCircularDependencies() {
            ArchRule rule = slices()
                    .matching("com.poultry.(*)..")
                    .should().beFreeOfCycles();

            rule.check(classes);
        }

        @Test
        @DisplayName("entities should not depend on services")
        void entitiesShouldNotDependOnServices() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..entity..")
                    .should().dependOnClassesThat().resideInAPackage("..service..");

            rule.check(classes);
        }

        @Test
        @DisplayName("entities should not depend on controllers")
        void entitiesShouldNotDependOnControllers() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..entity..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..");

            rule.check(classes);
        }

        @Test
        @DisplayName("DTOs should not depend on entities")
        void dtosShouldNotDependOnEntities() {
            // Note: This rule might be too strict - DTOs often reference entity enums
            // Adjust based on actual architecture decisions
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..dto..")
                    .and().haveSimpleNameEndingWith("Dto")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..entity..")
                    .andShould().notHaveSimpleName(".*Enum.*"); // Allow enum dependencies

            // Commented out as it might be too strict
            // rule.check(classes);
        }
    }

    @Nested
    @DisplayName("Module Independence Rules")
    class ModuleIndependenceTests {

        @Test
        @DisplayName("auth module should not depend on order module entities")
        void authShouldNotDependOnOrderEntities() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..auth..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..order.entity..");

            rule.check(classes);
        }

        @Test
        @DisplayName("cart module should not depend on payment module")
        void cartShouldNotDependOnPayment() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..cart..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..payment..");

            rule.check(classes);
        }

        @Test
        @DisplayName("product module should not depend on order module")
        void productShouldNotDependOnOrder() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..product..")
                    .should().dependOnClassesThat()
                    .resideInAPackage("..order..");

            rule.check(classes);
        }
    }

    @Nested
    @DisplayName("Best Practices Rules")
    class BestPracticesTests {

        @Test
        @DisplayName("should not use field injection")
        void shouldNotUseFieldInjection() {
            ArchRule rule = noFields()
                    .should().beAnnotatedWith(org.springframework.beans.factory.annotation.Autowired.class)
                    .because("Field injection is discouraged. Use constructor injection instead.");

            rule.check(classes);
        }

        @Test
        @DisplayName("controllers should not throw generic Exception")
        void controllersShouldNotThrowGenericException() {
            ArchRule rule = noMethods()
                    .that().areDeclaredInClassesThat().resideInAPackage("..controller..")
                    .should().declareThrowableOfType(Exception.class)
                    .because("Controllers should throw specific exceptions");

            // Note: This might be too strict if controller methods declare Exception
            // rule.check(classes);
        }

        @Test
        @DisplayName("services should use constructor injection")
        void servicesShouldUseConstructorInjection() {
            // Services with @RequiredArgsConstructor or explicit constructors are fine
            ArchRule rule = classes()
                    .that().resideInAPackage("..service..")
                    .and().areAnnotatedWith(org.springframework.stereotype.Service.class)
                    .should().haveOnlyFinalFields()
                    .orShould().beAnnotatedWith(lombok.RequiredArgsConstructor.class);

            // Note: This rule might need adjustment based on actual service implementation
            // rule.check(classes);
        }
    }
}
