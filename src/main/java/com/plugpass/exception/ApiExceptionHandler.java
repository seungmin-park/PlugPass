package com.plugpass.exception;

import com.plugpass.common.response.ApiError;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidRequest(MethodArgumentNotValidException exception) {
        Map<String,String> fields = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparingInt(error -> "FiniteDouble".equals(error.getCode()) ? 0 : 1))
                .forEach(error -> fields.putIfAbsent(error.getField(),message(error)));
        return ResponseEntity.badRequest().body(new ApiError("INVALID_REQUEST","입력값을 확인해주세요",fields));
    }
    private String message(FieldError error) {
        if (error.isBindingFailure()) { return "올바른 형식으로 입력해야 합니다"; }
        return error.getDefaultMessage();
    }
}
