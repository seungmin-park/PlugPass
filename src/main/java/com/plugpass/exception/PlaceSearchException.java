package com.plugpass.exception;

public final class PlaceSearchException extends RuntimeException {
    public enum Failure {
        NOT_CONFIGURED("주소 검색 준비 중입니다. 지도나 현재 위치로 검색해 주세요"),
        AUTHENTICATION("주소 검색 연결 설정을 확인하고 있습니다. 지도나 현재 위치로 검색해 주세요"),
        RATE_LIMIT("주소 검색 요청이 많습니다. 잠시 후 다시 시도해 주세요"),
        TIMEOUT("주소 검색 응답이 늦어지고 있습니다. 다시 시도해 주세요"),
        UPSTREAM("주소 검색 서비스에 연결하지 못했습니다. 다시 시도해 주세요"),
        INVALID_RESPONSE("주소 검색 응답을 확인하지 못했습니다. 다시 시도해 주세요");
        private final String message;
        Failure(String message) { this.message = message; }
    }
    private final Failure failure;
    public PlaceSearchException(Failure failure) { super(failure.message); this.failure = failure; }
    public Failure failure() { return failure; }
    public String code() { return "PLACE_SEARCH_" + failure.name(); }
}
