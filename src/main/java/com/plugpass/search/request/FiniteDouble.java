package com.plugpass.search.request;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FiniteDoubleValidator.class)
public @interface FiniteDouble {
    String message() default "좌표는 유한수여야 합니다";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
