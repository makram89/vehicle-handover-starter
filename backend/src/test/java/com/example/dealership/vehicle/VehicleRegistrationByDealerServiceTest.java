package com.example.dealership.vehicle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import com.example.dealership.error.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleRegistrationByDealerServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    // A-R3, A-Q7, scenariusz 3
    @Test
    void updatesRegistrationByDealer() {
        Vehicle vehicle = new Vehicle(
                "WVWZZZ1KZAW000001", "Volkswagen", "Golf", 2025, FuelType.PETROL, LocalDate.of(2026, 12, 15));
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        // zapis może iść przez save albo przez dirty checking, test nie wymusza żadnego z nich
        lenient().when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.updateRegistrationByDealer(
                1L, new RegistrationByDealerRequest(true));

        assertThat(response.registrationByDealer()).isTrue();
        assertThat(response.vin()).isEqualTo("WVWZZZ1KZAW000001");
        assertThat(response.plannedHandoverDate()).isEqualTo(LocalDate.of(2026, 12, 15));
        assertThat(vehicle.isRegistrationByDealer()).isTrue();
    }

    // A-R3 (flagę można też wyłączyć)
    @Test
    void updatesRegistrationByDealerBackToFalse() {
        Vehicle vehicle = new Vehicle(
                "WVWZZZ1KZAW000001", "Volkswagen", "Golf", 2025, FuelType.PETROL, null);
        vehicle.setRegistrationByDealer(true);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
        lenient().when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VehicleResponse response = vehicleService.updateRegistrationByDealer(
                1L, new RegistrationByDealerRequest(false));

        assertThat(response.registrationByDealer()).isFalse();
        assertThat(vehicle.isRegistrationByDealer()).isFalse();
    }

    // scenariusz 19
    @Test
    void throwsNotFoundWhenVehicleMissing() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleService.updateRegistrationByDealer(
                99L, new RegistrationByDealerRequest(true)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Vehicle with id 99 not found");
        verify(vehicleRepository, never()).save(any());
    }
}
