package com.plugpass.common.validation;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
public class FiniteDoubleValidator implements ConstraintValidator<FiniteDouble,Double> {
    public boolean isValid(Double value, ConstraintValidatorContext context) { return value == null || Double.isFinite(value); }
}
