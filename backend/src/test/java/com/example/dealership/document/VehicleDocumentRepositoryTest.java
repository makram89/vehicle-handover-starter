package com.example.dealership.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import com.example.dealership.vehicle.FuelType;
import com.example.dealership.vehicle.Vehicle;
import com.example.dealership.vehicle.VehicleRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

@DataJpaTest
class VehicleDocumentRepositoryTest {

    private static final String VIN = "WF0XXXGCDX0000004";
    private static final String OTHER_VIN = "WF0XXXGCDX0000005";
    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 11, 14);

    @Autowired
    private VehicleDocumentRepository vehicleDocumentRepository;

    @Autowired
    private VehicleRepository vehicleRepository;

    @Autowired
    private TestEntityManager entityManager;

    // A-R4, A-Q11 (50 znaków)
    @Test
    void saveStoresDocumentThatCanBeReadBack() {
        Long vehicleId = vehicle(VIN).getId();
        String number = "N".repeat(50);
        VehicleDocument saved = vehicleDocumentRepository.save(new VehicleDocument(
                vehicleId, DocumentType.REGISTRATION_POA, number, ISSUE_DATE, LocalDate.of(2027, 11, 13),
                DocumentStatus.PENDING));
        entityManager.flush();
        entityManager.clear();

        VehicleDocument found = vehicleDocumentRepository.findById(saved.getId()).orElseThrow();

        assertThat(found).isNotSameAs(saved);
        assertThat(found.getVehicleId()).isEqualTo(vehicleId);
        assertThat(found.getType()).isEqualTo(DocumentType.REGISTRATION_POA);
        assertThat(found.getNumber()).isEqualTo(number);
        assertThat(found.getIssueDate()).isEqualTo(ISSUE_DATE);
        assertThat(found.getExpiryDate()).isEqualTo(LocalDate.of(2027, 11, 13));
        assertThat(found.getStatus()).isEqualTo(DocumentStatus.PENDING);
    }

    // A-R8
    @Test
    void saveStoresDocumentWithoutExpiryDate() {
        Long vehicleId = vehicle(VIN).getId();
        VehicleDocument saved = vehicleDocumentRepository.save(new VehicleDocument(
                vehicleId, DocumentType.SALES_AGREEMENT, "UM/1", ISSUE_DATE, null, DocumentStatus.VERIFIED));
        entityManager.flush();
        entityManager.clear();

        VehicleDocument found = vehicleDocumentRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getExpiryDate()).isNull();
        assertThat(found.getStatus()).isEqualTo(DocumentStatus.VERIFIED);
    }

    @Test
    void findByVehicleIdReturnsOnlyDocumentsOfThatVehicle() {
        Long first = vehicle(VIN).getId();
        Long second = vehicle(OTHER_VIN).getId();
        VehicleDocument a = document(first, DocumentType.INVOICE, "FV/1");
        VehicleDocument b = document(second, DocumentType.INVOICE, "FV/2");
        VehicleDocument c = document(first, DocumentType.COC, "COC/3");
        entityManager.flush();
        entityManager.clear();

        List<VehicleDocument> found = vehicleDocumentRepository.findByVehicleId(first, Sort.by("id"));

        assertThat(found).extracting(VehicleDocument::getId).containsExactly(a.getId(), c.getId());
        assertThat(found).extracting(VehicleDocument::getVehicleId).containsOnly(first);
        assertThat(vehicleDocumentRepository.findByVehicleId(second, Sort.by("id")))
                .extracting(VehicleDocument::getId).containsExactly(b.getId());
    }

    @Test
    void findByIdAndVehicleIdDoesNotFindDocumentOfAnotherVehicle() {
        Long first = vehicle(VIN).getId();
        Long second = vehicle(OTHER_VIN).getId();
        VehicleDocument saved = document(first, DocumentType.INVOICE, "FV/1");
        entityManager.flush();
        entityManager.clear();

        assertThat(vehicleDocumentRepository.findByIdAndVehicleId(saved.getId(), first)).isPresent();
        assertThat(vehicleDocumentRepository.findByIdAndVehicleId(saved.getId(), second)).isEmpty();
    }

    // brak UNIQUE w bazie (A-R11 sprawdza serwis, odrzucone zostają w historii)
    @Test
    void saveAllowsDocumentsWithTheSameTypeForOneVehicle() {
        Long vehicleId = vehicle(VIN).getId();
        document(vehicleId, DocumentType.INVOICE, "FV/1");
        document(vehicleId, DocumentType.INVOICE, "FV/2");
        entityManager.flush();
        entityManager.clear();

        assertThat(vehicleDocumentRepository.findByVehicleId(vehicleId, Sort.by("id"))).hasSize(2);
    }

    // A-Q11 (bez unikalności numeru)
    @Test
    void saveAllowsDocumentsWithTheSameNumber() {
        Long first = vehicle(VIN).getId();
        Long second = vehicle(OTHER_VIN).getId();
        document(first, DocumentType.INVOICE, "FV/1");
        document(second, DocumentType.INVOICE, "FV/1");
        document(first, DocumentType.COC, "FV/1");
        entityManager.flush();
        entityManager.clear();

        assertThat(vehicleDocumentRepository.findByVehicleId(first, Sort.by("id"))).hasSize(2);
        assertThat(vehicleDocumentRepository.findByVehicleId(second, Sort.by("id"))).hasSize(1);
    }

    // A-Q12 / kontrakt (FK vehicle_id)
    @Test
    void saveRejectsDocumentOfUnknownVehicle() {
        assertThatThrownBy(() -> vehicleDocumentRepository.saveAndFlush(new VehicleDocument(
                Long.MAX_VALUE, DocumentType.INVOICE, "FV/1", ISSUE_DATE, null, DocumentStatus.PENDING)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // A-Q12 (kaskada)
    @Test
    void deletingVehicleDeletesItsDocuments() {
        Vehicle vehicle = vehicle(VIN);
        Long other = vehicle(OTHER_VIN).getId();
        document(vehicle.getId(), DocumentType.INVOICE, "FV/1");
        document(vehicle.getId(), DocumentType.COC, "COC/2");
        document(other, DocumentType.INVOICE, "FV/3");
        entityManager.flush();

        vehicleRepository.delete(vehicle);
        entityManager.flush();
        entityManager.clear();

        assertThat(vehicleDocumentRepository.findByVehicleId(vehicle.getId(), Sort.by("id"))).isEmpty();
        assertThat(vehicleDocumentRepository.findByVehicleId(other, Sort.by("id"))).hasSize(1);
    }

    private Vehicle vehicle(String vin) {
        return vehicleRepository.saveAndFlush(new Vehicle(vin, "Ford", "Focus", 2026, FuelType.ELECTRIC, null));
    }

    private VehicleDocument document(Long vehicleId, DocumentType type, String number) {
        return vehicleDocumentRepository.save(
                new VehicleDocument(vehicleId, type, number, ISSUE_DATE, null, DocumentStatus.PENDING));
    }
}
