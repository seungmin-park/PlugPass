package com.plugpass.recommendation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RecommendationRouteTests {
    @Autowired private MockMvc mockMvc;
    @Test
    @DisplayName("후보가 없는 추천 요청도 세 그룹의 빈 목록을 반환한다")
    void returnsEmptyGroups() throws Exception {
        mockMvc.perform(get("/api/v1/recommendations").param("latitude","0").param("longitude","0")
                .param("radiusMeters","1000").param("connector","DC_COMBO"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.preferred").isEmpty())
                .andExpect(jsonPath("$.requiresConfirmation").isEmpty()).andExpect(jsonPath("$.excluded").isEmpty());
    }
}
