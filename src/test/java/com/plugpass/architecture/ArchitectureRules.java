package com.plugpass.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

final class ArchitectureRules {
    private ArchitectureRules() { }

    static void checkControllerDependencies(JavaClasses importedClasses) {
        noClasses().that().areAnnotatedWith(RestController.class)
                .should().dependOnClassesThat(DescribedPredicate.describe("repositories, EntityManager or concrete services",
                        target -> isPersistence(target) || target.isAnnotatedWith(Service.class)))
                .because("controllers translate HTTP through service contracts").check(importedClasses);
    }

    static void checkDomainDependencies(JavaClasses importedClasses) {
        noClasses().that(DescribedPredicate.describe("domain objects", ArchitectureRules::isDomain))
                .should().dependOnClassesThat(DescribedPredicate.describe("HTTP, persistence access or application services",
                        target -> isPersistence(target) || isService(target) || isHttp(target)))
                .because("domain objects own their invariants without HTTP or I/O coordination").check(importedClasses);
    }

    static void checkEntityMutation(JavaClasses importedClasses) {
        methods().that().areDeclaredInClassesThat().areAnnotatedWith(Entity.class).and().arePublic()
                .should().haveNameNotMatching("set[A-Z].*")
                .because("entities expose intentional operations instead of arbitrary public setters").check(importedClasses);
    }

    static void checkServiceContracts(JavaClasses importedClasses) {
        classes().that().areAnnotatedWith(Service.class).should(new ArchCondition<>("implement the corresponding Default service contract") {
            @Override
            public void check(JavaClass service, ConditionEvents events) {
                String name = service.getSimpleName();
                String contract = name.startsWith("Default") ? name.substring("Default".length()) : "";
                boolean valid = contract.endsWith("Service") && service.getAllRawInterfaces().stream()
                        .anyMatch(serviceInterface -> serviceInterface.getSimpleName().equals(contract));
                events.add(new SimpleConditionEvent(service, valid, service.getName() + " must be Default<contract> implementing <contract>"));
            }
        }).check(importedClasses);
    }

    static void checkServiceTests(JavaClasses importedClasses) {
        classes().that(DescribedPredicate.describe("Spring integration tests using services", candidate ->
                candidate.isAnnotatedWith(SpringBootTest.class) && candidate.getDirectDependenciesFromSelf().stream()
                        .anyMatch(dependency -> isService(dependency.getTargetClass()))))
                .should(new ArchCondition<>("use real services without test transactions or Mockito") {
                    @Override
                    public void check(JavaClass serviceTest, ConditionEvents events) {
                        if (hasTransaction(serviceTest)) {
                            events.add(SimpleConditionEvent.violated(serviceTest, serviceTest.getName() + " has a test transaction"));
                        }
                        for (JavaMethod method : serviceTest.getAllMethods()) {
                            if (method.isAnnotatedWith(Transactional.class) || method.isAnnotatedWith("jakarta.transaction.Transactional")) {
                                events.add(SimpleConditionEvent.violated(method, method.getFullName() + " has a test transaction"));
                            }
                        }
                        serviceTest.getDirectDependenciesFromSelf().stream()
                                .filter(dependency -> isMockito(dependency.getTargetClass()))
                                .forEach(dependency -> events.add(SimpleConditionEvent.violated(dependency, dependency.getDescription())));
                    }
                }).because("test transactions and service mocks can hide missing production commits").check(importedClasses);
    }

    static void checkMvcTests(JavaClasses importedClasses) {
        noClasses().that().areAnnotatedWith(WebMvcTest.class)
                .should().dependOnClassesThat(DescribedPredicate.describe("repositories or EntityManager", ArchitectureRules::isPersistence))
                .because("MVC slices own HTTP contracts and substitute lower layers").check(importedClasses);
    }

    private static boolean isPersistence(JavaClass target) {
        return target.isAssignableTo(Repository.class) || target.isAssignableTo("jakarta.persistence.EntityManager");
    }

    private static boolean isService(JavaClass target) {
        return target.isAnnotatedWith(Service.class) || target.isInterface() && target.getSimpleName().endsWith("Service");
    }

    private static boolean isDomain(JavaClass candidate) {
        return candidate.isAnnotatedWith(Entity.class) ||
                (candidate.getPackageName().startsWith("com.plugpass.station") || candidate.getPackageName().startsWith("com.plugpass.freshness"))
                        && !isPersistence(candidate) && !candidate.isAnnotatedWith(Configuration.class);
    }

    private static boolean isHttp(JavaClass target) {
        return target.getPackageName().startsWith("org.springframework.web.") ||
                target.getPackageName().startsWith("com.plugpass.") && target.getPackageName().matches(".*\\.(request|response)(\\..*)?");
    }

    private static boolean hasTransaction(JavaClass target) {
        return target.isAnnotatedWith(Transactional.class) || target.isAnnotatedWith("jakarta.transaction.Transactional");
    }

    private static boolean isMockito(JavaClass target) {
        return target.getPackageName().startsWith("org.mockito") ||
                target.getPackageName().startsWith("org.springframework.test.context.bean.override.mockito") ||
                target.getPackageName().startsWith("org.springframework.boot.test.mock.mockito");
    }
}
