package com.plugpass.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ConnectorSelectionValidator.class)
public @interface ValidConnectorSelection {
    String message() default "연결 규격을 확인해 주세요";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
