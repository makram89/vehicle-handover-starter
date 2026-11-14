package com.example.dealership;

import java.time.Clock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class DealershipApplication {

    public static void main(String[] args) {
        SpringApplication.run(DealershipApplication.class, args);
    }

    // Declared here (not in a separate @Configuration) so that @WebMvcTest slices also get a Clock.
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }
}
