package com.plugpass.exception;

public class PublicDataException extends RuntimeException {
    private final PublicDataFailure failure;
    public PublicDataException(PublicDataFailure failure) {
        super("Public data request failed: " + failure);
        this.failure = failure;
    }
    public PublicDataFailure getFailure() {
        return failure;
    }
}
