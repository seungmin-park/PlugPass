package com.plugpass.exception;
import java.time.Duration;
public class PublicDataException extends RuntimeException {
    private final PublicDataFailure failure;
    private final Duration retryAfter;
    public PublicDataException(PublicDataFailure failure) { this(failure,null); }
    public PublicDataException(PublicDataFailure failure, Duration retryAfter) {
        super("Public data request failed: " + failure);
        this.failure = failure;
        this.retryAfter = retryAfter;
    }
    public PublicDataFailure getFailure() { return failure; }
    public Duration getRetryAfter() { return retryAfter; }
}
