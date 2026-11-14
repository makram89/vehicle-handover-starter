package com.example.dealership.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.dealership.error.NotFoundException;
import com.example.dealership.vehicle.FuelType;
import com.example.dealership.vehicle.Vehicle;
import com.example.dealership.vehicle.VehicleRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

// "Dziś" = 2026-11-14 (FixedClockTestConfig). Auto X: plannedHandoverDate 2026-12-15, registrationByDealer = false.
@ExtendWith(MockitoExtension.class)
class HandoverReadinessServiceTest {

    private static final LocalDate HANDOVER_DATE = LocalDate.of(2026, 12, 15);

    @Mock
    private VehicleRepository vehicleRepository;

    @Mock
    private VehicleDocumentRepository vehicleDocumentRepository;

    private HandoverReadinessService service;

    @BeforeEach
    void setUp() {
        service = new HandoverReadinessService(
                vehicleRepository, vehicleDocumentRepository, FixedClockTestConfig.CLOCK);
    }

    // A-R1, scenariusz 1
    @Test
    void reportsFourRequiredTypesMissingWhenNoDocuments() {
        givenVehicle(HANDOVER_DATE, false);
        givenDocuments();

        assertThat(service.check(1L)).isEqualTo(new HandoverReadinessResponse(false,
                List.of(DocumentType.SALES_AGREEMENT, DocumentType.INVOICE, DocumentType.COC,
                        DocumentType.OC_POLICY),
                List.of()));
    }

    // A-R2, scenariusz 2
    @Test
    void requiresRegistrationPoaWhenRegistrationByDealer() {
        givenVehicle(HANDOVER_DATE, true);
        givenDocuments();

        assertThat(service.check(1L)).isEqualTo(new HandoverReadinessResponse(false,
                List.of(DocumentType.SALES_AGREEMENT, DocumentType.INVOICE, DocumentType.COC,
                        DocumentType.OC_POLICY, DocumentType.REGISTRATION_POA),
                List.of()));
    }

    // A-R12, scenariusz 11
    @Test
    void treatsRejectedDocumentAsMissing() {
        givenVehicle(HANDOVER_DATE, false);
        givenDocuments(
                doc(DocumentType.SALES_AGREEMENT, DocumentStatus.VERIFIED, null),
                doc(DocumentType.INVOICE, DocumentStatus.VERIFIED, null),
                doc(DocumentType.COC, DocumentStatus.REJECTED, null),
                doc(DocumentType.OC_POLICY, DocumentStatus.VERIFIED, null));

        assertThat(service.check(1L)).isEqualTo(
                new HandoverReadinessResponse(false, List.of(DocumentType.COC), List.of()));
    }

    // A-R11, A-R12 (odrzucony w historii nie przesłania aktywnego dokumentu tego samego typu)
    @Test
    void usesActiveDocumentWhenRejectedOneOfSameTypeExists() {
        givenVehicle(HANDOVER_DATE, false);
        givenDocuments(
                doc(DocumentType.INVOICE, DocumentStatus.REJECTED, null),
                doc(DocumentType.SALES_AGREEMENT, DocumentStatus.VERIFIED, null),
                doc(DocumentType.INVOICE, DocumentStatus.VERIFIED, null),
                doc(DocumentType.COC, DocumentStatus.VERIFIED, null),
                doc(DocumentType.OC_POLICY, DocumentStatus.VERIFIED, null));

        assertThat(service.check(1L)).isEqualTo(new HandoverReadinessResponse(true, List.of(), List.of()));
    }

    // A-R9, scenariusz 12 (granica: ważny włącznie z dniem wydania)
    @Test
    void isReadyWhenExpiryDateEqualsHandoverDate() {
        givenVehicle(HANDOVER_DATE, false);
        givenDocuments(completeSetWithOcPolicyExpiry(HANDOVER_DATE));

        assertThat(service.check(1L)).isEqualTo(new HandoverReadinessResponse(true, List.of(), List.of()));
    }

    // A-R9, scenariusz 13
    @Test
    void reportsExpiredWhenExpiryDateBeforeHandoverDate() {
        givenVehicle(HANDOVER_DATE, false);
        givenDocuments(completeSetWithOcPolicyExpiry(LocalDate.of(2026, 12, 14)));

        assertThat(service.check(1L)).isEqualTo(new HandoverReadinessResponse(false, List.of(),
                List.of(new ReadinessProblem(DocumentType.OC_POLICY, ReadinessReason.EXPIRED))));
    }

    // A-R8, scenariusz 14
    @Test
    void doesNotReportExpiredForDocumentWithoutExpiryDate() {
        givenVehicle(HANDOVER_DATE, false);
        givenDocuments(
                doc(DocumentType.SALES_AGREEMENT, DocumentStatus.VERIFIED, null),
                doc(DocumentType.INVOICE, DocumentStatus.VERIFIED, null),
                doc(DocumentType.COC, DocumentStatus.VERIFIED, LocalDate.of(2030, 1, 1)),
                doc(DocumentType.OC_POLICY, DocumentStatus.VERIFIED, LocalDate.of(2027, 1, 1)));

        assertThat(service.check(1L)).isEqualTo(new HandoverReadinessResponse(true, List.of(), List.of()));
    }

    // A-Q2, scenariusz 15
    @Test
    void reportsNotVerifiedForPendingDocument() {
        givenVehicle(HANDOVER_DATE, false);
        givenDocuments(
                doc(DocumentType.SALES_AGREEMENT, DocumentStatus.VERIFIED, null),
                doc(DocumentType.INVOICE, DocumentStatus.PENDING, null),
                doc(DocumentType.COC, DocumentStatus.VERIFIED, null),
                doc(DocumentType.OC_POLICY, DocumentStatus.VERIFIED, null));

        assertThat(service.check(1L)).isEqualTo(new HandoverReadinessResponse(false, List.of(),
                List.of(new ReadinessProblem(DocumentType.INVOICE, ReadinessReason.NOT_VERIFIED))));
    }

    // A-Q2 (ten sam PENDING dokument: NOT_VERIFIED przed EXPIRED)
    @Test
    void reportsNotVerifiedBeforeExpiredForSamePendingDocument() {
        givenVehicle(HANDOVER_DATE, false);
        givenDocuments(
                doc(DocumentType.SALES_AGREEMENT, DocumentStatus.VERIFIED, null),
                doc(DocumentType.INVOICE, DocumentStatus.VERIFIED, null),
                doc(DocumentType.COC, DocumentStatus.VERIFIED, null),
                doc(DocumentType.OC_POLICY, DocumentStatus.PENDING, LocalDate.of(2026, 12, 14)));

        assertThat(service.check(1L)).isEqualTo(new HandoverReadinessResponse(false, List.of(), List.of(
                new ReadinessProblem(DocumentType.OC_POLICY, ReadinessReason.NOT_VERIFIED),
                new ReadinessProblem(DocumentType.OC_POLICY, ReadinessReason.EXPIRED))));
    }

    // A-R14, A-Q4, scenariusz 16 (bez daty wydania ważność liczona względem dziś, NO_HANDOVER_DATE na końcu)
    @Test
    void reportsExpiredAgainstTodayAndNoHandoverDateLastWhenHandoverDateMissing() {
        givenVehicle(null, false);
        givenDocuments(completeSetWithOcPolicyExpiry(LocalDate.of(2026, 11, 13)));

        assertThat(service.check(1L)).isEqualTo(new HandoverReadinessResponse(false, List.of(), List.of(
                new ReadinessProblem(DocumentType.OC_POLICY, ReadinessReason.EXPIRED),
                new ReadinessProblem(null, ReadinessReason.NO_HANDOVER_DATE))));
    }

    // A-R9, A-Q4 (granica: bez daty wydania dziś jeszcze się liczy)
    @Test
    void doesNotReportExpiredWhenExpiryDateIsTodayAndHandoverDateMissing() {
        givenVehicle(null, false);
        givenDocuments(completeSetWithOcPolicyExpiry(LocalDate.of(2026, 11, 14)));

        assertThat(service.check(1L)).isEqualTo(new HandoverReadinessResponse(false, List.of(),
                List.of(new ReadinessProblem(null, ReadinessReason.NO_HANDOVER_DATE))));
    }

    // A-Q8, scenariusz 17
    @Test
    void ignoresRegistrationPoaWhenNotRequired() {
        givenVehicle(HANDOVER_DATE, false);
        givenDocuments(
                doc(DocumentType.SALES_AGREEMENT, DocumentStatus.VERIFIED, null),
                doc(DocumentType.INVOICE, DocumentStatus.VERIFIED, null),
                doc(DocumentType.COC, DocumentStatus.VERIFIED, null),
                doc(DocumentType.OC_POLICY, DocumentStatus.VERIFIED, null),
                doc(DocumentType.REGISTRATION_POA, DocumentStatus.PENDING, LocalDate.of(2026, 1, 1)));

        assertThat(service.check(1L)).isEqualTo(new HandoverReadinessResponse(true, List.of(), List.of()));
    }

    // A-Q13 (data wydania w przeszłości: ważność względem niej, nie względem dziś)
    @Test
    void usesHandoverDateEvenWhenInPast() {
        givenVehicle(LocalDate.of(2026, 11, 1), false);
        givenDocuments(
                doc(DocumentType.SALES_AGREEMENT, DocumentStatus.VERIFIED, null),
                doc(DocumentType.INVOICE, DocumentStatus.VERIFIED, null),
                doc(DocumentType.COC, DocumentStatus.VERIFIED, null),
                doc(DocumentType.OC_POLICY, DocumentStatus.VERIFIED, LocalDate.of(2026, 11, 2)));

        assertThat(service.check(1L)).isEqualTo(new HandoverReadinessResponse(true, List.of(), List.of()));
    }

    // §4 pkt 3 (kolejność problemów = kolejność deklaracji DocumentType, nie kolejność id dokumentów)
    @Test
    void ordersProblemsByDocumentTypeDeclaration() {
        givenVehicle(HANDOVER_DATE, false);
        givenDocuments(
                doc(DocumentType.OC_POLICY, DocumentStatus.PENDING, null),
                doc(DocumentType.INVOICE, DocumentStatus.VERIFIED, null),
                doc(DocumentType.COC, DocumentStatus.VERIFIED, null),
                doc(DocumentType.SALES_AGREEMENT, DocumentStatus.VERIFIED, LocalDate.of(2026, 12, 14)));

        assertThat(service.check(1L)).isEqualTo(new HandoverReadinessResponse(false, List.of(), List.of(
                new ReadinessProblem(DocumentType.SALES_AGREEMENT, ReadinessReason.EXPIRED),
                new ReadinessProblem(DocumentType.OC_POLICY, ReadinessReason.NOT_VERIFIED))));
    }

    // scenariusz 19
    @Test
    void throwsNotFoundWhenVehicleMissing() {
        when(vehicleRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.check(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Vehicle with id 99 not found");
        verifyNoInteractions(vehicleDocumentRepository);
    }

    private void givenVehicle(LocalDate plannedHandoverDate, boolean registrationByDealer) {
        Vehicle vehicle = new Vehicle(
                "WVWZZZ1KZAW000001", "Volkswagen", "Golf", 2025, FuelType.PETROL, plannedHandoverDate);
        vehicle.setRegistrationByDealer(registrationByDealer);
        when(vehicleRepository.findById(1L)).thenReturn(Optional.of(vehicle));
    }

    private void givenDocuments(VehicleDocument... documents) {
        when(vehicleDocumentRepository.findByVehicleId(1L, Sort.by("id"))).thenReturn(List.of(documents));
    }

    private void givenDocuments(List<VehicleDocument> documents) {
        when(vehicleDocumentRepository.findByVehicleId(1L, Sort.by("id"))).thenReturn(documents);
    }

    private static List<VehicleDocument> completeSetWithOcPolicyExpiry(LocalDate ocPolicyExpiry) {
        List<VehicleDocument> documents = new ArrayList<>();
        documents.add(doc(DocumentType.SALES_AGREEMENT, DocumentStatus.VERIFIED, null));
        documents.add(doc(DocumentType.INVOICE, DocumentStatus.VERIFIED, null));
        documents.add(doc(DocumentType.COC, DocumentStatus.VERIFIED, null));
        documents.add(doc(DocumentType.OC_POLICY, DocumentStatus.VERIFIED, ocPolicyExpiry));
        return documents;
    }

    private static VehicleDocument doc(DocumentType type, DocumentStatus status, LocalDate expiryDate) {
        return new VehicleDocument(1L, type, "NR/" + type, LocalDate.of(2026, 1, 15), expiryDate, status);
    }
}
