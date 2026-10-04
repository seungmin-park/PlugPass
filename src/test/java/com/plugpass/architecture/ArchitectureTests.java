package com.plugpass.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ArchitectureTests {
    private static final JavaClasses productionClasses = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS).importPackages("com.plugpass");
    private static final JavaClasses projectClasses = new ClassFileImporter().importPackages("com.plugpass");

    @Test
    @DisplayName("Controller는 Service 계약을 통해 HTTP 유스케이스를 호출한다")
    void controllersUseServiceContracts() { ArchitectureRules.checkControllerDependencies(productionClasses); }

    @Test
    @DisplayName("도메인은 HTTP와 저장 호출 경계에 의존하지 않는다")
    void domainsOwnInvariantsWithoutIoDependencies() { ArchitectureRules.checkDomainDependencies(productionClasses); }

    @Test
    @DisplayName("엔티티는 공개 setter 대신 의도가 있는 변경 메서드를 제공한다")
    void entitiesExposeIntentionalMutation() { ArchitectureRules.checkEntityMutation(productionClasses); }

    @Test
    @DisplayName("Service 구현은 Default 이름과 대응 유스케이스 계약을 갖는다")
    void servicesImplementNamedContracts() { ArchitectureRules.checkServiceContracts(productionClasses); }

    @Test
    @DisplayName("Service 통합 테스트는 테스트 트랜잭션과 Mockito 없이 실제 서비스를 검증한다")
    void serviceTestsVerifyProductionTransactions() { ArchitectureRules.checkServiceTests(projectClasses); }

    @Test
    @DisplayName("MVC slice 테스트는 실제 Repository를 의존하지 않는다")
    void mvcTestsOwnHttpBoundary() { ArchitectureRules.checkMvcTests(projectClasses); }
}
