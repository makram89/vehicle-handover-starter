package com.example.dealership.document;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

// Stały "dziś" = 2026-11-14 z INTENT_A.md §5. Bean ma inną nazwę niż produkcyjny `clock` i jest @Primary,
// więc działa i w @WebMvcTest (jedyny Clock), i w @SpringBootTest (obok ClockConfig).
@TestConfiguration(proxyBeanMethods = false)
public class FixedClockTestConfig {

    static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-14T10:00:00Z"), ZoneOffset.UTC);
    static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    @Bean
    @Primary
    Clock fixedClock() {
        return CLOCK;
    }
}
