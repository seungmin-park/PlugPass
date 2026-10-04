package com.plugpass.architecture;

import architecturefixture.RuleSamples.ConcreteServiceController;
import architecturefixture.RuleSamples.ContractController;
import architecturefixture.RuleSamples.DefaultMissingService;
import architecturefixture.RuleSamples.DefaultSampleService;
import architecturefixture.RuleSamples.HttpEntity;
import architecturefixture.RuleSamples.MethodTransactionalServiceTest;
import architecturefixture.RuleSamples.MockedMvcTest;
import architecturefixture.RuleSamples.MockedServiceTest;
import architecturefixture.RuleSamples.RealServiceTest;
import architecturefixture.RuleSamples.RepositoryController;
import architecturefixture.RuleSamples.RepositoryEntity;
import architecturefixture.RuleSamples.RepositoryMvcTest;
import architecturefixture.RuleSamples.SampleServiceImpl;
import architecturefixture.RuleSamples.SetterEntity;
import architecturefixture.RuleSamples.TransactionalServiceTest;
import architecturefixture.RuleSamples.UpdatingEntity;
import com.plugpass.recommendation.domain.InvalidCandidatePolicy;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchitectureRulesTests {
    @Test
    @DisplayName("추천 정책 도메인이 Repository에 의존하면 거부한다")
    void rejectsRecommendationPolicyRepositoryDependency() {
        JavaClasses classes = new ClassFileImporter().importClasses(UpdatingEntity.class, InvalidCandidatePolicy.class);
        assertThatThrownBy(() -> ArchitectureRules.checkDomainDependencies(classes))
                .isInstanceOf(AssertionError.class).hasMessageContaining("InvalidCandidatePolicy");
    }

    @Test
    @DisplayName("Controller가 Repository를 직접 의존하면 거부한다")
    void rejectsControllerRepositoryDependency() {
        JavaClasses classes = new ClassFileImporter().importClasses(RepositoryController.class);
        assertThatThrownBy(() -> ArchitectureRules.checkControllerDependencies(classes))
                .isInstanceOf(AssertionError.class).hasMessageContaining("RepositoryController");
    }

    @Test
    @DisplayName("Controller가 Service 구현에 직접 의존하면 거부한다")
    void rejectsControllerConcreteServiceDependency() {
        JavaClasses classes = new ClassFileImporter().importClasses(ConcreteServiceController.class);
        assertThatThrownBy(() -> ArchitectureRules.checkControllerDependencies(classes))
                .isInstanceOf(AssertionError.class).hasMessageContaining("ConcreteServiceController");
    }

    @Test
    @DisplayName("Controller가 Service 계약에 의존하는 구조는 허용한다")
    void allowsControllerServiceContract() {
        JavaClasses classes = new ClassFileImporter().importClasses(ContractController.class);
        ArchitectureRules.checkControllerDependencies(classes);
    }

    @Test
    @DisplayName("도메인이 Repository를 의존하면 거부한다")
    void rejectsDomainRepositoryDependency() {
        JavaClasses classes = new ClassFileImporter().importClasses(RepositoryEntity.class);
        assertThatThrownBy(() -> ArchitectureRules.checkDomainDependencies(classes))
                .isInstanceOf(AssertionError.class).hasMessageContaining("RepositoryEntity");
    }

    @Test
    @DisplayName("도메인이 HTTP 응답 DTO를 의존하면 거부한다")
    void rejectsDomainHttpDependency() {
        JavaClasses classes = new ClassFileImporter().importClasses(HttpEntity.class);
        assertThatThrownBy(() -> ArchitectureRules.checkDomainDependencies(classes))
                .isInstanceOf(AssertionError.class).hasMessageContaining("HttpEntity");
    }

    @Test
    @DisplayName("엔티티의 공개 setter는 거부한다")
    void rejectsPublicEntitySetter() {
        JavaClasses classes = new ClassFileImporter().importClasses(SetterEntity.class);
        assertThatThrownBy(() -> ArchitectureRules.checkEntityMutation(classes))
                .isInstanceOf(AssertionError.class).hasMessageContaining("setName");
    }

    @Test
    @DisplayName("엔티티의 의도를 드러내는 변경 메서드는 허용한다")
    void allowsIntentionalEntityMutation() {
        JavaClasses classes = new ClassFileImporter().importClasses(UpdatingEntity.class);
        ArchitectureRules.checkEntityMutation(classes);
        ArchitectureRules.checkDomainDependencies(classes);
    }

    @Test
    @DisplayName("Service 구현이 Default 이름과 대응 계약을 갖추면 허용한다")
    void allowsDefaultServiceContract() {
        JavaClasses classes = new ClassFileImporter().importClasses(DefaultSampleService.class);
        ArchitectureRules.checkServiceContracts(classes);
    }

    @Test
    @DisplayName("Impl 이름의 Service 구현은 거부한다")
    void rejectsImplServiceName() {
        JavaClasses classes = new ClassFileImporter().importClasses(SampleServiceImpl.class);
        assertThatThrownBy(() -> ArchitectureRules.checkServiceContracts(classes))
                .isInstanceOf(AssertionError.class).hasMessageContaining("SampleServiceImpl");
    }

    @Test
    @DisplayName("Default 이름만 있고 대응 Service 계약이 없으면 거부한다")
    void rejectsServiceWithoutContract() {
        JavaClasses classes = new ClassFileImporter().importClasses(DefaultMissingService.class);
        assertThatThrownBy(() -> ArchitectureRules.checkServiceContracts(classes))
                .isInstanceOf(AssertionError.class).hasMessageContaining("DefaultMissingService");
    }

    @Test
    @DisplayName("Service 테스트의 클래스 트랜잭션은 거부한다")
    void rejectsServiceTestClassTransaction() {
        JavaClasses classes = new ClassFileImporter().importClasses(TransactionalServiceTest.class);
        assertThatThrownBy(() -> ArchitectureRules.checkServiceTests(classes))
                .isInstanceOf(AssertionError.class).hasMessageContaining("TransactionalServiceTest");
    }

    @Test
    @DisplayName("Service 테스트의 메서드 트랜잭션은 거부한다")
    void rejectsServiceTestMethodTransaction() {
        JavaClasses classes = new ClassFileImporter().importClasses(MethodTransactionalServiceTest.class);
        assertThatThrownBy(() -> ArchitectureRules.checkServiceTests(classes))
                .isInstanceOf(AssertionError.class).hasMessageContaining("verifyCommit");
    }

    @Test
    @DisplayName("Service 테스트의 Mockito 사용은 거부한다")
    void rejectsServiceTestMock() {
        JavaClasses classes = new ClassFileImporter().importClasses(MockedServiceTest.class);
        assertThatThrownBy(() -> ArchitectureRules.checkServiceTests(classes))
                .isInstanceOf(AssertionError.class).hasMessageContaining("MockedServiceTest");
    }

    @Test
    @DisplayName("실제 Service 계약을 호출하고 테스트 트랜잭션이 없는 구조는 허용한다")
    void allowsRealServiceTest() {
        JavaClasses classes = new ClassFileImporter().importClasses(RealServiceTest.class);
        ArchitectureRules.checkServiceTests(classes);
    }

    @Test
    @DisplayName("MVC slice 테스트의 실제 Repository 의존은 거부한다")
    void rejectsMvcRepositoryDependency() {
        JavaClasses classes = new ClassFileImporter().importClasses(RepositoryMvcTest.class);
        assertThatThrownBy(() -> ArchitectureRules.checkMvcTests(classes))
                .isInstanceOf(AssertionError.class).hasMessageContaining("RepositoryMvcTest");
    }

    @Test
    @DisplayName("MVC slice 테스트에서 Service mock을 사용하는 구조는 허용한다")
    void allowsMvcServiceMock() {
        JavaClasses classes = new ClassFileImporter().importClasses(MockedMvcTest.class);
        ArchitectureRules.checkMvcTests(classes);
    }

}
