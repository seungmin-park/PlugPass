package com.plugpass.search.controller;

import com.plugpass.search.domain.MapSearchArea;
import com.plugpass.search.domain.StationAreaSearchQuery;
import com.plugpass.search.dto.StationSearchResult;
import com.plugpass.search.service.StationQueryService;
import com.plugpass.ingestion.service.CollectionStatusService;
import com.plugpass.station.domain.Connector;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;

@WebMvcTest(StationController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class StationAreaHttpTests {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private StationQueryService stationQueryService;
    @MockitoBean private CollectionStatusService collectionStatusService;
    @Test
    @DisplayName("지도 경계와 기본 limit을 전달하고 빈 응답 계약을 문서화한다")
    void documentsArea() throws Exception {
        StationSearchResult result = mock(StationSearchResult.class);
        when(result.stations()).thenReturn(List.of());
        when(stationQueryService.searchArea(any())).thenReturn(result);
        mockMvc.perform(get("/api/v1/stations/area").queryParams(parameters()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.dataReady").value(false))
            .andExpect(jsonPath("$.lastSuccessfulRunAt").isEmpty()).andExpect(jsonPath("$.stations").isEmpty())
            .andDo(document("station-area", queryParameters(parameterWithName("south").description("남쪽 위도, 경계 포함"),
                parameterWithName("north").description("북쪽 위도"), parameterWithName("west").description("서쪽 경도"),
                parameterWithName("east").description("동쪽 경도"), parameterWithName("connector").description("차량 커넥터"),
                parameterWithName("limit").optional().description("1~50, 기본20")), responseFields(
                subsectionWithPath("collection").optional().description("수집 작업 상태 CURRENT/DELAYED/NOT_READY, 전체·상태 성공/최근 시도 시각과 300초 갱신 주기; 현장 관측 최신성 보장 아님"), fieldWithPath("dataReady").description("전체 수집 성공 이력"), fieldWithPath("lastSuccessfulRunAt").optional().description("마지막 전체 성공 시각"),
                fieldWithPath("stations").description("중심 거리·ID순 충전소; 기존 주변 검색과 동일한 원소 계약"))));
        verify(stationQueryService).searchArea(new StationAreaSearchQuery(new MapSearchArea(37.49, 37.51, 126.99, 127.01), Connector.DC_COMBO, 20));
    }
    @ParameterizedTest
    @DisplayName("영역·limit·커넥터의 잘못된 값은 서비스 호출 전에 거부한다")
    @CsvSource({"south,37.52,areaValid", "north,38,areaValid", "west,128,areaValid", "east,181,areaValid", "south,NaN,south", "north,Infinity,north", "limit,0,limit", "limit,51,limit", "connector,wrong,connector", "west,text,west"})
    void rejectsInvalidArea(String field, String value, String errorField) throws Exception {
        MultiValueMap<String, String> parameters = parameters(); parameters.set(field, value);
        mockMvc.perform(get("/api/v1/stations/area").queryParams(parameters)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("INVALID_REQUEST")).andExpect(jsonPath("$.fields." + errorField).isString());
        verifyNoInteractions(stationQueryService);
    }
    @Test
    @DisplayName("경계를 빠뜨리면 필수값 오류를 반환한다")
    void rejectsMissingBoundary() throws Exception {
        MultiValueMap<String, String> parameters = parameters(); parameters.remove("south");
        mockMvc.perform(get("/api/v1/stations/area").queryParams(parameters)).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.south").value("남쪽 경계는 필수입니다"));
        verifyNoInteractions(stationQueryService);
    }
    private MultiValueMap<String, String> parameters() {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        parameters.set("south", "37.49"); parameters.set("north", "37.51"); parameters.set("west", "126.99"); parameters.set("east", "127.01"); parameters.set("connector", "DC_COMBO"); return parameters;
    }
}
