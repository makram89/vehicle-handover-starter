package com.example.dealership.vehicle;

import jakarta.validation.constraints.NotNull;

public record RegistrationByDealerRequest(
        @NotNull
        Boolean registrationByDealer
) {
}
