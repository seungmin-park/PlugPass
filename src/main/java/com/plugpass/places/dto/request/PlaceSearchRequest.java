package com.plugpass.places.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
public record PlaceSearchRequest(@NotBlank(message = "주소나 장소를 입력해 주세요")
        @Size(max = 100, message = "검색어는 100자 이하여야 합니다") String query) {
    public PlaceSearchRequest { if (query != null) query = query.strip(); }
}
