package com.example.dealership.vehicle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class VehicleRegistrationByDealerRepositoryTest {

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TestEntityManager entityManager;

    // A-R3 (domyślnie klient rejestruje sam)
    @Test
    void newVehicleDefaultsRegistrationByDealerToFalse() {
        Vehicle saved = vehicleRepository.save(
                new Vehicle("ZTDCFLW0000000005", "Ford", "Focus", 2026, FuelType.ELECTRIC, null));
        entityManager.flush();
        entityManager.clear();

        Vehicle found = vehicleRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.isRegistrationByDealer()).isFalse();
    }

    // A-R3 (zapis flagi przetrwa odczyt z bazy)
    @Test
    void registrationByDealerIsPersisted() {
        Vehicle vehicle = new Vehicle("ZTDCFLW0000000008", "Ford", "Puma", 2025, FuelType.HYBRID, null);
        vehicle.setRegistrationByDealer(true);
        Vehicle saved = vehicleRepository.save(vehicle);
        entityManager.flush();
        entityManager.clear();

        assertThat(vehicleRepository.findById(saved.getId()).orElseThrow().isRegistrationByDealer()).isTrue();
    }

    // A-R3 (migracja V7 ma DEFAULT FALSE dla istniejących wierszy z seeda)
    @Test
    void seededVehiclesHaveRegistrationByDealerFalse() {
        assertThat(vehicleRepository.findAll()).isNotEmpty()
                .allSatisfy(vehicle -> assertThat(vehicle.isRegistrationByDealer()).isFalse());
    }
}
