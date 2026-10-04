package com.plugpass.recommendation;

import java.util.List;
import java.util.stream.Stream;
import com.plugpass.search.Connector;
import com.plugpass.search.StationSearchQuery;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RecommendationController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class RecommendationHttpTests {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private RecommendationService recommendationService;
    @Test
    @DisplayName("추천 그룹과 이유를 JSON으로 반환하고 검색 조건을 전달한다")
    void documentsRecommendation() throws Exception {
        RankedCandidate preferred = mock(RankedCandidate.class);
        when(preferred.id()).thenReturn(12L); when(preferred.name()).thenReturn("최신 후보");
        when(preferred.distanceMeters()).thenReturn(123.5); when(preferred.reasonCodes()).thenReturn(List.of("RECENT_AVAILABLE"));
        RankedCandidate confirmation = mock(RankedCandidate.class);
        when(confirmation.id()).thenReturn(13L); when(confirmation.name()).thenReturn("확인 후보");
        when(confirmation.distanceMeters()).thenReturn(50.0); when(confirmation.reasonCodes()).thenReturn(List.of("UNVERIFIED_AVAILABLE","SOURCE_OBSERVED_AT_MISSING"));
        RankedCandidate excluded = mock(RankedCandidate.class);
        when(excluded.id()).thenReturn(14L); when(excluded.name()).thenReturn("제외 후보");
        when(excluded.distanceMeters()).thenReturn(25.0); when(excluded.reasonCodes()).thenReturn(List.of("ACCESS_RESTRICTED"));
        when(recommendationService.recommend(any(),eq(10L))).thenReturn(new CandidateGroups(List.of(preferred),List.of(confirmation),List.of(excluded)));
        mockMvc.perform(get("/api/v1/recommendations").param("latitude","0").param("longitude","0")
                .param("radiusMeters","1000").param("connector","DC_COMBO").param("excludeStationId","10"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.preferred[0].id").value(12))
                .andExpect(jsonPath("$.preferred[0].name").value("최신 후보"))
                .andExpect(jsonPath("$.preferred[0].distanceMeters").value(123.5))
                .andExpect(jsonPath("$.preferred[0].reasonCodes[0]").value("RECENT_AVAILABLE"))
                .andExpect(jsonPath("$.requiresConfirmation[0].id").value(13))
                .andExpect(jsonPath("$.excluded[0].reasonCodes[0]").value("ACCESS_RESTRICTED"))
                .andDo(document("recommendations",queryParameters(
                    parameterWithName("latitude").description("위도 -90~90"),parameterWithName("longitude").description("경도 -180~180"),
                    parameterWithName("radiusMeters").description("직선 반경 100~10000m"),parameterWithName("connector").description("필수 커넥터"),
                    parameterWithName("limit").optional().description("각 그룹 최대 1~50, 기본20"),
                    parameterWithName("excludeStationId").optional().description("제외할 내부 충전소 ID, 양수")),responseFields(
                    fieldWithPath("preferred").description("RECENT·AVAILABLE·명시적 제한 없는 후보"),
                    fieldWithPath("requiresConfirmation").description("UNVERIFIED·AVAILABLE 후보"),fieldWithPath("excluded").description("제외 후보와 이유"),
                    fieldWithPath("preferred[].id").description("내부 충전소 ID"),
                    fieldWithPath("preferred[].name").description("충전소명"),fieldWithPath("preferred[].distanceMeters").description("직선거리 m"),
                    fieldWithPath("preferred[].reasonCodes").description("판단 이유·운영 조건 경고"),
                    fieldWithPath("requiresConfirmation[].id").description("내부 충전소 ID"),fieldWithPath("requiresConfirmation[].name").description("충전소명"),
                    fieldWithPath("requiresConfirmation[].distanceMeters").description("직선거리 m"),fieldWithPath("requiresConfirmation[].reasonCodes").description("미확인 근거"),
                    fieldWithPath("excluded[].id").description("내부 충전소 ID"),fieldWithPath("excluded[].name").description("충전소명"),
                    fieldWithPath("excluded[].distanceMeters").description("직선거리 m"),fieldWithPath("excluded[].reasonCodes").description("제외 이유"))));
        verify(recommendationService).recommend(new StationSearchQuery(new GeoPoint(0,0),1000,Connector.DC_COMBO,20),10L);
    }
    @ParameterizedTest
    @DisplayName("잘못된 추천 입력은 400과 필드 오류로 거부한다")
    @MethodSource("invalidInputs")
    void rejectsInvalidInput(String field, String value, String message) throws Exception {
        mockMvc.perform(get("/api/v1/recommendations").param("latitude",field.equals("latitude") ? value : "0")
                .param("longitude",field.equals("longitude") ? value : "0")
                .param("radiusMeters",field.equals("radiusMeters") ? value : "1000")
                .param("connector",field.equals("connector") ? value : "DC_COMBO")
                .param("limit",field.equals("limit") ? value : "20")
                .param("excludeStationId",field.equals("excludeStationId") ? value : "1"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.fields."+field).value(message));
        verifyNoInteractions(recommendationService);
    }
    static Stream<Arguments> invalidInputs() {
        return Stream.of(Arguments.of("latitude","91","위도는 90 이하여야 합니다"),Arguments.of("latitude","NaN","좌표는 유한수여야 합니다"),
                Arguments.of("longitude","181","경도는 180 이하여야 합니다"),Arguments.of("radiusMeters","99","반경은 100m 이상이어야 합니다"),
                Arguments.of("connector","BAD","올바른 형식으로 입력해야 합니다"),Arguments.of("limit","51","limit은 50 이하여야 합니다"),
                Arguments.of("excludeStationId","0","제외 충전소 ID는 양수여야 합니다"));
    }
    @Test
    @DisplayName("필수 좌표 누락은 400으로 거부한다")
    void rejectsMissingCoordinates() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations").param("radiusMeters","1000").param("connector","DC_COMBO"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.fields.latitude").value("위도는 필수입니다"));
        verifyNoInteractions(recommendationService);
    }
}
