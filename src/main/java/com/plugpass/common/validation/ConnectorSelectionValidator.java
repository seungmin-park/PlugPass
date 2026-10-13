package com.plugpass.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public final class ConnectorSelectionValidator implements ConstraintValidator<ValidConnectorSelection, ConnectorSelectionInput> {
    @Override
    public boolean isValid(ConnectorSelectionInput input, ConstraintValidatorContext context) {
        if (input == null) { return true; }
        if (input.connector() != null && input.connectors() != null) {
            return violation(context, "connector", "단일 규격과 여러 규격을 함께 지정할 수 없습니다");
        }
        if (input.connector() == null && (input.connectors() == null || input.connectors().isEmpty())) {
            return violation(context, "connector", "커넥터는 필수입니다");
        }
        if (input.connectors() != null && input.connectors().contains(null)) {
            return violation(context, "connectors", "지원하는 연결 규격을 선택해 주세요");
        }
        return true;
    }

    private boolean violation(ConstraintValidatorContext context, String field, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message).addPropertyNode(field).addConstraintViolation();
        return false;
    }
}
