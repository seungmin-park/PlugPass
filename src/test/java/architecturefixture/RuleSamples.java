package architecturefixture;

import com.plugpass.search.response.StationSearchResponse;
import com.plugpass.station.StationRepository;
import jakarta.persistence.Entity;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.stereotype.Service;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RestController;

// Bytecode inputs outside com.plugpass so component/entity scanning cannot load them.
public final class RuleSamples {
    private RuleSamples() { }
    public interface SampleService { }
    @Service public static class DefaultSampleService implements SampleService { }
    @Service public static class SampleServiceImpl implements SampleService { }
    @Service public static class DefaultMissingService { }
    @RestController public static class RepositoryController { StationRepository stationRepository; }
    @RestController public static class ConcreteServiceController { DefaultSampleService sampleService; }
    @RestController public static class ContractController { SampleService sampleService; }
    @Entity public static class RepositoryEntity { StationRepository stationRepository; }
    @Entity public static class HttpEntity { StationSearchResponse response; }
    @Entity public static class SetterEntity { public void setName(String name) { } }
    @Entity public static class UpdatingEntity { public void update(String name) { } }
    @SpringBootTest @Transactional public static class TransactionalServiceTest { SampleService sampleService; }
    @SpringBootTest public static class MethodTransactionalServiceTest {
        SampleService sampleService;
        @Transactional void verifyCommit() { }
    }
    @SpringBootTest public static class MockedServiceTest { @Mock SampleService sampleService; }
    @SpringBootTest public static class RealServiceTest { SampleService sampleService; }
    @WebMvcTest public static class RepositoryMvcTest { StationRepository stationRepository; }
    @WebMvcTest public static class MockedMvcTest { @MockitoBean SampleService sampleService; }
}
