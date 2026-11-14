package com.example.dealership.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class NotInFutureValidatorTest {

    private final NotInFutureValidator validator = new NotInFutureValidator(FixedClockTestConfig.CLOCK);

    // A-R10: null łapie @NotNull
    @Test
    void acceptsNull() {
        assertThat(validator.isValid(null, null)).isTrue();
    }

    // A-R10, scenariusz 4 (granica: dziś jest dozwolone)
    @Test
    void acceptsToday() {
        assertThat(validator.isValid(LocalDate.of(2026, 11, 14), null)).isTrue();
    }

    // A-R10
    @Test
    void acceptsPastDate() {
        assertThat(validator.isValid(LocalDate.of(2026, 11, 13), null)).isTrue();
    }

    // A-R10, scenariusz 5
    @Test
    void rejectsTomorrow() {
        assertThat(validator.isValid(LocalDate.of(2026, 11, 15), null)).isFalse();
    }

    // A-R10 (komunikat z intentu §3)
    @Test
    void usesMessageFromIntent() throws Exception {
        assertThat(NotInFuture.class.getMethod("message").getDefaultValue())
                .isEqualTo("must not be in the future");
    }
}
