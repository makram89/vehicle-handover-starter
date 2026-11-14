package com.example.dealership.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

class ExpiryNotBeforeIssueValidatorTest {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 11, 14);

    // Domyślna fabryka nie umie utworzyć NotInFutureValidator (wymaga Clock), więc podajemy stały Clock.
    private final Validator validator = Validation.byDefaultProvider().configure()
            .constraintValidatorFactory(new ConstraintValidatorFactory() {
                @Override
                public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
                    try {
                        if (key == NotInFutureValidator.class) {
                            return key.cast(new NotInFutureValidator(FixedClockTestConfig.CLOCK));
                        }
                        return key.getDeclaredConstructor().newInstance();
                    } catch (ReflectiveOperationException e) {
                        throw new IllegalStateException(e);
                    }
                }

                @Override
                public void releaseInstance(ConstraintValidator<?, ?> instance) {
                }
            })
            .buildValidatorFactory()
            .getValidator();

    // A-Q10, scenariusz 18 (naruszenie na węźle expiryDate, żeby 400 miał field)
    @Test
    void reportsViolationOnExpiryDateWhenExpiryIsBeforeIssue() {
        Set<ConstraintViolation<DocumentRequest>> violations =
                validator.validate(request(ISSUE_DATE, LocalDate.of(2026, 11, 13)));

        assertThat(violations).hasSize(1);
        ConstraintViolation<DocumentRequest> violation = violations.iterator().next();
        assertThat(violation.getPropertyPath().toString()).isEqualTo("expiryDate");
        assertThat(violation.getMessage()).isEqualTo("must not be before issue date");
    }

    // A-Q10 (granica: równe daty są dozwolone)
    @Test
    void acceptsExpiryEqualToIssue() {
        assertThat(validator.validate(request(ISSUE_DATE, ISSUE_DATE))).isEmpty();
    }

    @Test
    void acceptsExpiryAfterIssue() {
        assertThat(validator.validate(request(ISSUE_DATE, LocalDate.of(2027, 11, 13)))).isEmpty();
    }

    // A-R8: expiryDate opcjonalna
    @Test
    void acceptsMissingExpiry() {
        assertThat(validator.validate(request(ISSUE_DATE, null))).isEmpty();
    }

    // brak issueDate łapie @NotNull, a nie ta reguła
    @Test
    void ignoresMissingIssue() {
        Set<ConstraintViolation<DocumentRequest>> violations =
                validator.validate(request(null, LocalDate.of(2026, 11, 13)));

        assertThat(violations).extracting(violation -> violation.getPropertyPath().toString())
                .doesNotContain("expiryDate");
    }

    private static DocumentRequest request(LocalDate issueDate, LocalDate expiryDate) {
        return new DocumentRequest(DocumentType.INVOICE, "FV/2026/0001", issueDate, expiryDate);
    }
}
