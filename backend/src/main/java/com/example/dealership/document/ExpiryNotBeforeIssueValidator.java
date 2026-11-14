package com.example.dealership.document;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ExpiryNotBeforeIssueValidator implements ConstraintValidator<ExpiryNotBeforeIssue, DocumentRequest> {

    @Override
    public boolean isValid(DocumentRequest request, ConstraintValidatorContext context) {
        if (request == null || request.issueDate() == null || request.expiryDate() == null
                || !request.expiryDate().isBefore(request.issueDate())) {
            return true;
        }
        // Report on the expiryDate node so the 400 response carries field = "expiryDate".
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
                .addPropertyNode("expiryDate")
                .addConstraintViolation();
        return false;
    }
}
