package com.plugpass.exception;
public class StationNotFoundException extends RuntimeException {
    public StationNotFoundException() { super("충전소를 찾을 수 없습니다"); }
}
