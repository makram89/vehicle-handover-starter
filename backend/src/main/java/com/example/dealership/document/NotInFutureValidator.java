package com.example.dealership.document;

import java.time.Clock;
import java.time.LocalDate;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotInFutureValidator implements ConstraintValidator<NotInFuture, LocalDate> {

    private final Clock clock;

    public NotInFutureValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        return value == null || !value.isAfter(LocalDate.now(clock));
    }
}
