package com.plugpass.search;

import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;
import com.plugpass.freshness.Freshness;
import com.plugpass.freshness.FreshnessAssessment;
import com.plugpass.station.ChargerDetails;
import com.plugpass.station.ChargerStatus;
import com.plugpass.station.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StationController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class StationSearchHttpTests {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private StationQueryService stationQueryService;

    @Test
    @DisplayName("검색 응답의 값과 기본 limit 전달을 확인하고 REST Docs 계약을 만든다")
    void documentsSearch() throws Exception {
        Instant collectedAt = Instant.parse("2026-10-02T00:00:00Z");
        ChargerDetails details = new ChargerDetails("04","24시간","Y","입주민 전용",null,"20190829121020",null,null,null);
        ChargerObservation charger = new ChargerObservation("01",ChargerStatus.AVAILABLE,"2",null,collectedAt,new FreshnessAssessment(Freshness.UNVERIFIED,"SOURCE_OBSERVED_AT_MISSING"),details);
        StationMatch station = new StationMatch(7L,"ME","source-one","충전소",new GeoPoint(37.5,126.6),12.5,List.of(charger));
        StationSearchResult result = mock(StationSearchResult.class);
        when(result.dataReady()).thenReturn(true);
        when(result.lastSuccessfulRunAt()).thenReturn(collectedAt);
        when(result.stations()).thenReturn(List.of(station));
        when(stationQueryService.search(any())).thenReturn(result);
        mockMvc.perform(get("/api/v1/stations").queryParam("latitude","37.5").queryParam("longitude","126.6")
                .queryParam("radiusMeters","1000").queryParam("connector","DC_COMBO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dataReady").value(true))
                .andExpect(jsonPath("$.lastSuccessfulRunAt").value("2026-10-02T00:00:00Z"))
                .andExpect(jsonPath("$.stations.length()").value(1)).andExpect(jsonPath("$.stations[0].id").value(7))
                .andExpect(jsonPath("$.stations[0].distanceMeters").value(12.5))
                .andExpect(jsonPath("$.stations[0].reportedAvailableCount").value(1))
                .andExpect(jsonPath("$.stations[0].compatibleChargerCount").value(1))
                .andExpect(jsonPath("$.stations[0].chargers[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.stations[0].chargers[0].freshness").value("UNVERIFIED"))
                .andExpect(jsonPath("$.stations[0].chargers[0].reasonCode").value("SOURCE_OBSERVED_AT_MISSING"))
                .andExpect(jsonPath("$.stations[0].chargers[0].limitYn").value("Y"))
                .andExpect(jsonPath("$.stations[0].chargers[0].limitDetail").value("입주민 전용"))
                .andDo(document("station-search",queryParameters(
                        parameterWithName("latitude").description("위도 -90~90"), parameterWithName("longitude").description("경도 -180~180"),
                        parameterWithName("radiusMeters").description("직선 검색 반경 100~10000m, 경계 포함"),
                        parameterWithName("connector").description("DC_CHADEMO, AC_SLOW, AC_THREE_PHASE, DC_COMBO, NACS"),
                        parameterWithName("limit").optional().description("1~50, 기본20")),
                        responseFields(fieldWithPath("dataReady").description("전체 수집 성공 이력이 있는지"),
                                fieldWithPath("lastSuccessfulRunAt").optional().description("마지막 전체 성공 시각, 없으면 null"),
                                fieldWithPath("stations").description("거리/내부ID순 충전소 목록"),
                                fieldWithPath("stations[].id").description("내부 충전소 ID"),fieldWithPath("stations[].provider").description("공급자"),
                                fieldWithPath("stations[].providerStationId").description("공급자 충전소 ID"),fieldWithPath("stations[].name").description("이름"),
                                fieldWithPath("stations[].latitude").description("위도"),fieldWithPath("stations[].longitude").description("경도"),
                                fieldWithPath("stations[].distanceMeters").description("직선거리 m, 주행거리 아님"),
                                fieldWithPath("stations[].reportedAvailableCount").description("호환 충전기 중 AVAILABLE 보고 수, 최신성/접근 가능 보장 아님"),
                                fieldWithPath("stations[].compatibleChargerCount").description("호환 충전기 수"),
                                subsectionWithPath("stations[].chargers").description("호환 충전기별 상태·최신성·시각·이용 조건; 본문 충전기 필드 계약 참조"))));
        verify(stationQueryService).search(new StationSearchQuery(new GeoPoint(37.5,126.6),1000,Connector.DC_COMBO,20));
    }
    @Test
    @DisplayName("데이터 준비 전 빈 결과도 200과 빈 배열로 응답한다")
    void returnsEmptySearch() throws Exception {
        StationSearchResult result = mock(StationSearchResult.class);
        when(result.stations()).thenReturn(List.of());
        when(stationQueryService.search(any())).thenReturn(result);
        mockMvc.perform(get("/api/v1/stations").queryParams(validParameters()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.dataReady").value(false))
                .andExpect(jsonPath("$.lastSuccessfulRunAt").isEmpty()).andExpect(jsonPath("$.stations").isEmpty());
    }
    @ParameterizedTest
    @DisplayName("검색 필수값 누락은 400과 필드별 오류로 거부한다")
    @MethodSource("missingParameters")
    void rejectsMissingParameter(String field, String message) throws Exception {
        MultiValueMap<String,String> parameters = validParameters(); parameters.remove(field);
        mockMvc.perform(get("/api/v1/stations").queryParams(parameters)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST")).andExpect(jsonPath("$.fields."+field).value(message));
        verifyNoInteractions(stationQueryService);
    }
    static Stream<Arguments> missingParameters() {
        return Stream.of(Arguments.of("latitude","위도는 필수입니다"),Arguments.of("longitude","경도는 필수입니다"),
                Arguments.of("radiusMeters","반경은 필수입니다"),Arguments.of("connector","커넥터는 필수입니다"));
    }
    @ParameterizedTest
    @DisplayName("좌표와 반경·limit·커넥터의 잘못된 입력은 서비스 호출 전에 거부한다")
    @MethodSource("invalidParameters")
    void rejectsInvalidParameter(String field, String value, String message) throws Exception {
        MultiValueMap<String,String> parameters = validParameters(); parameters.set(field,value);
        mockMvc.perform(get("/api/v1/stations").queryParams(parameters)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST")).andExpect(jsonPath("$.fields."+field).value(message));
        verifyNoInteractions(stationQueryService);
    }
    static Stream<Arguments> invalidParameters() {
        return Stream.of(Arguments.of("latitude","-90.1","위도는 -90 이상이어야 합니다"), Arguments.of("latitude","90.1","위도는 90 이하여야 합니다"),
                Arguments.of("longitude","-180.1","경도는 -180 이상이어야 합니다"), Arguments.of("longitude","180.1","경도는 180 이하여야 합니다"),
                Arguments.of("latitude","NaN","좌표는 유한수여야 합니다"),Arguments.of("longitude","Infinity","좌표는 유한수여야 합니다"),
                Arguments.of("radiusMeters","99","반경은 100m 이상이어야 합니다"),Arguments.of("radiusMeters","10001","반경은 10000m 이하여야 합니다"),
                Arguments.of("limit","0","limit은 1 이상이어야 합니다"),Arguments.of("limit","51","limit은 50 이하여야 합니다"),
                Arguments.of("connector","unknown","올바른 형식으로 입력해야 합니다"),Arguments.of("latitude","text","올바른 형식으로 입력해야 합니다"));
    }
    @ParameterizedTest
    @DisplayName("좌표·반경·limit의 최솟값과 최댓값은 허용한다")
    @MethodSource("validBoundaries")
    void acceptsBoundaries(String latitude, String longitude, String radius, String limit) throws Exception {
        StationSearchResult result = mock(StationSearchResult.class); when(result.stations()).thenReturn(List.of());
        when(stationQueryService.search(any())).thenReturn(result);
        MultiValueMap<String,String> parameters = validParameters(); parameters.set("latitude",latitude); parameters.set("longitude",longitude);
        parameters.set("radiusMeters",radius); parameters.set("limit",limit);
        mockMvc.perform(get("/api/v1/stations").queryParams(parameters)).andExpect(status().isOk());
        verify(stationQueryService).search(new StationSearchQuery(new GeoPoint(Double.parseDouble(latitude),Double.parseDouble(longitude)),Integer.parseInt(radius),Connector.DC_COMBO,Integer.parseInt(limit)));
    }
    static Stream<Arguments> validBoundaries() { return Stream.of(Arguments.of("-90","-180","100","1"),Arguments.of("90","180","10000","50")); }
    private MultiValueMap<String,String> validParameters() {
        MultiValueMap<String,String> parameters = new LinkedMultiValueMap<>(); parameters.set("latitude","37.5");parameters.set("longitude","126.6");
        parameters.set("radiusMeters","1000");parameters.set("connector","DC_COMBO");return parameters;
    }
}
