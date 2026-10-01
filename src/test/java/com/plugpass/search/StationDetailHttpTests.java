package com.plugpass.search;

import java.time.Instant;
import java.util.List;
import com.plugpass.exception.StationNotFoundException;
import com.plugpass.freshness.Freshness;
import com.plugpass.freshness.FreshnessAssessment;
import com.plugpass.station.ChargerDetails;
import com.plugpass.station.ChargerStatus;
import com.plugpass.station.GeoPoint;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(StationController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class StationDetailHttpTests {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private StationQueryService stationQueryService;

    @Test
    @DisplayName("상세의 상태·원본·UTC 시각·제한 정보 값과 REST Docs 모든 필드를 확인한다")
    void documentsDetail() throws Exception {
        Instant now = Instant.parse("2026-10-02T00:00:00Z");
        ChargerDetails details = new ChargerDetails("04","24시간","Y","입주민 전용","출입 확인","20190829121020","20210801121020","20210801123020","20210802131020","N","");
        ChargerObservation available = new ChargerObservation("01",ChargerStatus.AVAILABLE,"2",now,now,new FreshnessAssessment(Freshness.RECENT,"WITHIN_MAX_AGE"),details);
        ChargerObservation occupied = new ChargerObservation("02",ChargerStatus.OCCUPIED,"3",now.minusSeconds(601),now,new FreshnessAssessment(Freshness.STALE,"MAX_AGE_EXCEEDED"),details);
        ChargerObservation unknown = new ChargerObservation("03",ChargerStatus.UNKNOWN,"9",null,now,new FreshnessAssessment(Freshness.UNVERIFIED,"SOURCE_OBSERVED_AT_MISSING"),null);
        StationDetail detail = mock(StationDetail.class);
        when(detail.id()).thenReturn(7L); when(detail.provider()).thenReturn("ME");when(detail.providerStationId()).thenReturn("one");
        when(detail.name()).thenReturn("충전소");when(detail.location()).thenReturn(new GeoPoint(37.5,126.6));when(detail.chargers()).thenReturn(List.of(available,occupied,unknown));
        when(stationQueryService.detail(7L)).thenReturn(detail);
        mockMvc.perform(get("/api/v1/stations/{stationId}",7))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(7)).andExpect(jsonPath("$.providerStationId").value("one"))
                .andExpect(jsonPath("$.chargers.length()").value(3))
                .andExpect(jsonPath("$.chargers[0].status").value("AVAILABLE")).andExpect(jsonPath("$.chargers[0].rawStatus").value("2"))
                .andExpect(jsonPath("$.chargers[0].sourceObservedAt").value("2026-10-02T00:00:00Z"))
                .andExpect(jsonPath("$.chargers[0].collectedAt").value("2026-10-02T00:00:00Z"))
                .andExpect(jsonPath("$.chargers[0].freshness").value("RECENT"))
                .andExpect(jsonPath("$.chargers[0].limitYn").value("Y")).andExpect(jsonPath("$.chargers[0].limitDetail").value("입주민 전용"))
                .andExpect(jsonPath("$.chargers[0].note").value("출입 확인"))
                .andExpect(jsonPath("$.chargers[0].sourceStatusChangedAtRaw").value("20190829121020"))
                .andExpect(jsonPath("$.chargers[0].lastChargingStartedAtRaw").value("20210801121020"))
                .andExpect(jsonPath("$.chargers[0].lastChargingEndedAtRaw").value("20210801123020"))
                .andExpect(jsonPath("$.chargers[0].chargingStartedAtRaw").value("20210802131020"))
                .andExpect(jsonPath("$.chargers[0].delYn").value("N"))
                .andExpect(jsonPath("$.chargers[1].freshness").value("STALE"))
                .andExpect(jsonPath("$.chargers[2].freshness").value("UNVERIFIED"))
                .andExpect(jsonPath("$.chargers[2].reasonCode").value("SOURCE_OBSERVED_AT_MISSING"))
                .andDo(document("station-detail",pathParameters(parameterWithName("stationId").description("내부 충전소 ID, 공급자 ID와 다름")),responseFields(
                        fieldWithPath("id").description("내부 충전소 ID"),fieldWithPath("provider").description("공급자"),
                        fieldWithPath("providerStationId").description("공급자 충전소 ID"),fieldWithPath("name").description("이름"),
                        fieldWithPath("latitude").description("위도"),fieldWithPath("longitude").description("경도"),
                        fieldWithPath("chargers").description("충전기 번호순 전체 목록, 호환 필터 없음"),
                        fieldWithPath("chargers[].chargerId").description("공급자 충전기 번호"),fieldWithPath("chargers[].status").description("공급자 보고를 정규화한 상태"),
                        fieldWithPath("chargers[].rawStatus").type(JsonFieldType.STRING).optional().description("원본 상태 코드, 미제공 null"),
                        fieldWithPath("chargers[].sourceObservedAt").type(JsonFieldType.STRING).optional().description("신뢰 가능한 관측 시각, UTC offset ISO 8601, 미확인 null"),
                        fieldWithPath("chargers[].collectedAt").description("수집 시각, UTC offset ISO 8601, 최신성 근거 아님"),
                        fieldWithPath("chargers[].freshness").description("RECENT/STALE/UNVERIFIED"),fieldWithPath("chargers[].reasonCode").description("최신성 판정의 근거 코드"),
                        fieldWithPath("chargers[].connectorCode").type(JsonFieldType.STRING).optional().description("공급자 조합 원본 코드"),
                        fieldWithPath("chargers[].useTime").type(JsonFieldType.STRING).optional().description("원본 이용 시간, 미제공 null"),
                        fieldWithPath("chargers[].limitYn").type(JsonFieldType.STRING).optional().description("원본 제한 여부, 미제공 null; null을 제한 없음으로 추정하지 않음"),
                        fieldWithPath("chargers[].limitDetail").type(JsonFieldType.STRING).optional().description("원본 제한 내용"),
                        fieldWithPath("chargers[].note").type(JsonFieldType.STRING).optional().description("원본 비고"),
                        fieldWithPath("chargers[].sourceStatusChangedAtRaw").type(JsonFieldType.STRING).optional().description("상태 변경 원본 문자열, 시간대 미확인"),
                        fieldWithPath("chargers[].lastChargingStartedAtRaw").type(JsonFieldType.STRING).optional().description("마지막 충전 시작 원본"),
                        fieldWithPath("chargers[].lastChargingEndedAtRaw").type(JsonFieldType.STRING).optional().description("마지막 충전 종료 원본"),
                        fieldWithPath("chargers[].chargingStartedAtRaw").type(JsonFieldType.STRING).optional().description("현재 충전 시작 원본"),
                        fieldWithPath("chargers[].delYn").type(JsonFieldType.STRING).optional().description("삭제 플래그 원본, 자동 삭제하지 않음"),
                        fieldWithPath("chargers[].delDetail").type(JsonFieldType.STRING).optional().description("삭제 사유 원본"))));
        verify(stationQueryService).detail(7L);
    }
    @Test
    @DisplayName("미제공 운영 정보는 null과 미확인 이유를 그대로 응답한다")
    void preservesMissingInformation() throws Exception {
        ChargerObservation observation = new ChargerObservation("01",ChargerStatus.UNKNOWN,null,null,Instant.parse("2026-10-02T00:00:00Z"),new FreshnessAssessment(Freshness.UNVERIFIED,"SOURCE_OBSERVED_AT_MISSING"),null);
        StationDetail detail = mock(StationDetail.class);when(detail.id()).thenReturn(7L);when(detail.location()).thenReturn(new GeoPoint(37.5,126.6));when(detail.chargers()).thenReturn(List.of(observation));
        when(stationQueryService.detail(7L)).thenReturn(detail);
        mockMvc.perform(get("/api/v1/stations/7")).andExpect(status().isOk())
                .andExpect(jsonPath("$.chargers[0].rawStatus").isEmpty()).andExpect(jsonPath("$.chargers[0].sourceObservedAt").isEmpty())
                .andExpect(jsonPath("$.chargers[0].useTime").isEmpty()).andExpect(jsonPath("$.chargers[0].limitYn").isEmpty())
                .andExpect(jsonPath("$.chargers[0].note").isEmpty()).andExpect(jsonPath("$.chargers[0].reasonCode").value("SOURCE_OBSERVED_AT_MISSING"));
    }
    @ParameterizedTest
    @DisplayName("없는 ID는 404와 일관된 공개 오류로 반환한다")
    @ValueSource(longs = {0,-1,Long.MAX_VALUE})
    void returnsNotFound(long stationId) throws Exception {
        when(stationQueryService.detail(stationId)).thenThrow(new StationNotFoundException());
        mockMvc.perform(get("/api/v1/stations/{stationId}",stationId)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STATION_NOT_FOUND")).andExpect(jsonPath("$.message").value("충전소를 찾을 수 없습니다"))
                .andExpect(jsonPath("$.fields").isEmpty());
        verify(stationQueryService).detail(stationId);
    }
    @ParameterizedTest
    @DisplayName("숫자가 아니거나 Long 범위를 넘는 ID는 400으로 거부한다")
    @ValueSource(strings = {"text","9223372036854775808"})
    void rejectsInvalidPathId(String stationId) throws Exception {
        mockMvc.perform(get("/api/v1/stations/{stationId}",stationId)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST")).andExpect(jsonPath("$.fields.stationId").value("올바른 형식으로 입력해야 합니다"));
        verifyNoInteractions(stationQueryService);
    }
}
