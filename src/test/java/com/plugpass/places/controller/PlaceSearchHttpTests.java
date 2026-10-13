package com.plugpass.places.controller;

import com.plugpass.places.dto.PlaceMatch;
import com.plugpass.places.service.PlaceSearchService;
import com.plugpass.exception.PlaceSearchException;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;

@WebMvcTest(PlaceSearchController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
class PlaceSearchHttpTests {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private PlaceSearchService placeSearchService;
    @Test
    @DisplayName("공백을 제거한 검색어를 전달하고 위치 후보 JSON 계약을 문서화한다")
    void documentsPlaces() throws Exception {
        when(placeSearchService.search("서울 시청")).thenReturn(List.of(new PlaceMatch("서울 시청", "서울 주소", 37.5665, 126.978)));
        mockMvc.perform(get("/api/v1/places").queryParam("query", " 서울 시청 "))
            .andExpect(status().isOk()).andExpect(jsonPath("$.places.length()").value(1))
            .andExpect(jsonPath("$.places[0].name").value("서울 시청"))
            .andExpect(jsonPath("$.places[0].address").value("서울 주소"))
            .andExpect(jsonPath("$.places[0].latitude").value(37.5665)).andExpect(jsonPath("$.places[0].longitude").value(126.978))
            .andDo(document("place-search", queryParameters(parameterWithName("query").description("앞뒤 공백 제거 후 1~100자 주소·장소명")),
                responseFields(fieldWithPath("places").description("중복을 합친 위치 후보 최대10개; 충전소 목록 아님"),
                    fieldWithPath("places[].name").description("주소 또는 장소명"), fieldWithPath("places[].address").description("표시 주소"),
                    fieldWithPath("places[].latitude").description("위도"), fieldWithPath("places[].longitude").description("경도"))));
        verify(placeSearchService).search("서울 시청");
    }
    @ParameterizedTest
    @DisplayName("비어 있거나 너무 긴 검색어는 400 필드 오류로 거부한다")
    @ValueSource(ints = {0, 101})
    void rejectsInvalidLength(int length) throws Exception {
        mockMvc.perform(get("/api/v1/places").queryParam("query", "a".repeat(length)))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
            .andExpect(jsonPath("$.fields.query").isString());
        verifyNoInteractions(placeSearchService);
    }
    @Test
    @DisplayName("누락한 검색어는 외부 조회 없이 필수 안내로 거부한다")
    void rejectsMissingQuery() throws Exception {
        mockMvc.perform(get("/api/v1/places")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fields.query").value("주소나 장소를 입력해 주세요"));
        verifyNoInteractions(placeSearchService);
    }
    @ParameterizedTest
    @DisplayName("주소 검색 실패 원인을 공개 오류 코드와 HTTP 상태로 변환한다")
    @CsvSource({"NOT_CONFIGURED,503", "AUTHENTICATION,503", "RATE_LIMIT,429", "TIMEOUT,504", "UPSTREAM,502", "INVALID_RESPONSE,502"})
    void mapsExternalFailure(PlaceSearchException.Failure failure, int statusCode) throws Exception {
        PlaceSearchException exception = new PlaceSearchException(failure);
        when(placeSearchService.search("서울")).thenThrow(exception);
        mockMvc.perform(get("/api/v1/places").queryParam("query", "서울")).andExpect(status().is(statusCode))
            .andExpect(jsonPath("$.code").value("PLACE_SEARCH_" + failure.name()))
            .andExpect(jsonPath("$.message").value(exception.getMessage())).andExpect(jsonPath("$.fields").isEmpty());
    }
}
