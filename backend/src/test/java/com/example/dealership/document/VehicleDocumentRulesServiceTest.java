package com.example.dealership.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import com.example.dealership.error.ConflictException;
import com.example.dealership.error.NotFoundException;
import com.example.dealership.vehicle.VehicleRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class VehicleDocumentRulesServiceTest {

    private static final LocalDate ISSUE_DATE = LocalDate.of(2026, 11, 14);

    @Mock
    private VehicleDocumentRepository vehicleDocumentRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleDocumentService vehicleDocumentService;

    // A-R11, scenariusz 9 (aktywny PENDING; serwis pyta repozytorium o dokumenty o statusie innym niż REJECTED)
    @Test
    void createThrowsConflictWhenActivePendingDocumentOfTypeExists() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(vehicleDocumentRepository.existsByVehicleIdAndTypeAndStatusNot(1L, DocumentType.INVOICE,
                DocumentStatus.REJECTED)).thenReturn(true);

        assertThatThrownBy(() -> vehicleDocumentService.create(1L, request(DocumentType.INVOICE)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Vehicle with id 1 already has an active INVOICE document");
        verify(vehicleDocumentRepository, never()).save(any());
    }

    // A-R11, scenariusz 9 (aktywny VERIFIED; ta sama reguła, inny typ dla pewności, że komunikat zależy od typu)
    @Test
    void createThrowsConflictWhenActiveVerifiedDocumentOfTypeExists() {
        when(vehicleRepository.existsById(2L)).thenReturn(true);
        when(vehicleDocumentRepository.existsByVehicleIdAndTypeAndStatusNot(2L, DocumentType.COC,
                DocumentStatus.REJECTED)).thenReturn(true);

        assertThatThrownBy(() -> vehicleDocumentService.create(2L, request(DocumentType.COC)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Vehicle with id 2 already has an active COC document");
        verify(vehicleDocumentRepository, never()).save(any());
    }

    // A-R11, A-R12, scenariusz 10
    @Test
    void createSavesDocumentWhenOnlyRejectedDocumentOfTypeExists() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(vehicleDocumentRepository.existsByVehicleIdAndTypeAndStatusNot(1L, DocumentType.INVOICE,
                DocumentStatus.REJECTED)).thenReturn(false);
        when(vehicleDocumentRepository.save(any(VehicleDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = vehicleDocumentService.create(1L, request(DocumentType.INVOICE));

        assertThat(response.type()).isEqualTo(DocumentType.INVOICE);
        assertThat(response.status()).isEqualTo(DocumentStatus.PENDING);
        verify(vehicleDocumentRepository).save(any(VehicleDocument.class));
    }

    // A-R5, scenariusz 6
    @Test
    void changeStatusAllowsPendingToVerified() {
        assertTransitionAllowed(DocumentStatus.PENDING, DocumentStatus.VERIFIED);
    }

    // A-R5, scenariusz 6
    @Test
    void changeStatusAllowsPendingToRejected() {
        assertTransitionAllowed(DocumentStatus.PENDING, DocumentStatus.REJECTED);
    }

    // A-Q3, scenariusz 8
    @Test
    void changeStatusAllowsVerifiedToRejected() {
        assertTransitionAllowed(DocumentStatus.VERIFIED, DocumentStatus.REJECTED);
    }

    // A-R7, A-Q3, scenariusze 7 i 8 (wszystkie 6 zabronionych komórek macierzy)
    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "PENDING, PENDING",
            "VERIFIED, PENDING",
            "VERIFIED, VERIFIED",
            "REJECTED, PENDING",
            "REJECTED, VERIFIED",
            "REJECTED, REJECTED"
    })
    void changeStatusThrowsConflictForForbiddenTransition(DocumentStatus from, DocumentStatus to) {
        VehicleDocument document = givenDocument(from);

        assertThatThrownBy(() -> vehicleDocumentService.changeStatus(1L, 5L, new DocumentStatusRequest(to)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Cannot change document status from " + from + " to " + to);
        assertThat(document.getStatus()).isEqualTo(from);
        verify(vehicleDocumentRepository, never()).save(any());
    }

    // scenariusz 19 (404 pojazdu przed 404 dokumentu)
    @Test
    void changeStatusThrowsNotFoundWhenVehicleMissing() {
        when(vehicleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> vehicleDocumentService.changeStatus(99L, 5L,
                new DocumentStatusRequest(DocumentStatus.VERIFIED)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Vehicle with id 99 not found");
        verify(vehicleDocumentRepository, never()).findByIdAndVehicleId(any(), any());
        verify(vehicleDocumentRepository, never()).save(any());
    }

    // dokument innego auta: wyszukiwanie po parze (id dokumentu, id pojazdu z URL)
    @Test
    void changeStatusThrowsNotFoundWhenDocumentBelongsToOtherVehicle() {
        when(vehicleRepository.existsById(2L)).thenReturn(true);
        when(vehicleDocumentRepository.findByIdAndVehicleId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleDocumentService.changeStatus(2L, 5L,
                new DocumentStatusRequest(DocumentStatus.VERIFIED)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Document with id 5 not found");
        verify(vehicleDocumentRepository).findByIdAndVehicleId(5L, 2L);
        verify(vehicleDocumentRepository, never()).save(any());
    }

    private void assertTransitionAllowed(DocumentStatus from, DocumentStatus to) {
        VehicleDocument document = givenDocument(from);
        // zapis może iść przez save albo przez dirty checking, test nie wymusza żadnego z nich
        lenient().when(vehicleDocumentRepository.save(any(VehicleDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = vehicleDocumentService.changeStatus(1L, 5L, new DocumentStatusRequest(to));

        assertThat(response.status()).isEqualTo(to);
        assertThat(response.vehicleId()).isEqualTo(1L);
        assertThat(response.type()).isEqualTo(DocumentType.INVOICE);
        assertThat(document.getStatus()).isEqualTo(to);
    }

    private VehicleDocument givenDocument(DocumentStatus status) {
        VehicleDocument document = new VehicleDocument(
                1L, DocumentType.INVOICE, "FV/2026/0001", ISSUE_DATE, null, status);
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(vehicleDocumentRepository.findByIdAndVehicleId(5L, 1L)).thenReturn(Optional.of(document));
        return document;
    }

    private static DocumentRequest request(DocumentType type) {
        return new DocumentRequest(type, "NR/2026/0001", ISSUE_DATE, null);
    }
}
