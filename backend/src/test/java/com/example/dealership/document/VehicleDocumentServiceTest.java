package com.example.dealership.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.example.dealership.error.NotFoundException;
import com.example.dealership.vehicle.VehicleRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class VehicleDocumentServiceTest {

    private static final String VEHICLE_NOT_FOUND = "Vehicle with id 99 not found";

    @Mock
    private VehicleDocumentRepository vehicleDocumentRepository;

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleDocumentService vehicleDocumentService;

    @Test
    void findAllReturnsMappedDocuments() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(vehicleDocumentRepository.findByVehicleId(1L, Sort.by("id")))
                .thenReturn(List.of(document(DocumentType.INVOICE, "FV/1"), document(DocumentType.COC, "COC/2")));

        List<DocumentResponse> responses = vehicleDocumentService.findAll(1L);

        assertThat(responses).extracting(DocumentResponse::number).containsExactly("FV/1", "COC/2");
        assertThat(responses).extracting(DocumentResponse::vehicleId).containsOnly(1L);
    }

    // scenariusz 19
    @Test
    void findAllThrowsNotFoundWhenVehicleDoesNotExist() {
        when(vehicleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> vehicleDocumentService.findAll(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(VEHICLE_NOT_FOUND);
    }

    @Test
    void findByIdReturnsDocument() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(vehicleDocumentRepository.findByIdAndVehicleId(5L, 1L))
                .thenReturn(Optional.of(document(DocumentType.INVOICE, "FV/1")));

        DocumentResponse response = vehicleDocumentService.findById(1L, 5L);

        assertThat(response.number()).isEqualTo("FV/1");
        assertThat(response.vehicleId()).isEqualTo(1L);
    }

    @Test
    void findByIdThrowsNotFoundWhenDocumentDoesNotExist() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(vehicleDocumentRepository.findByIdAndVehicleId(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleDocumentService.findById(1L, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Document with id 5 not found");
    }

    // dokument innego auta: wyszukiwanie zawsze po parze (id dokumentu, id pojazdu z URL)
    @Test
    void findByIdThrowsNotFoundWhenDocumentBelongsToAnotherVehicle() {
        when(vehicleRepository.existsById(2L)).thenReturn(true);
        when(vehicleDocumentRepository.findByIdAndVehicleId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vehicleDocumentService.findById(2L, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Document with id 5 not found");
        verify(vehicleDocumentRepository).findByIdAndVehicleId(5L, 2L);
    }

    // scenariusz 19
    @Test
    void findByIdThrowsNotFoundWhenVehicleDoesNotExist() {
        when(vehicleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> vehicleDocumentService.findById(99L, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(VEHICLE_NOT_FOUND);
        verify(vehicleDocumentRepository, never()).findByIdAndVehicleId(any(), any());
    }

    // A-R4, A-Q5, A-Q6 (pełny zestaw pól), scenariusz 4
    @Test
    void createSavesDocumentWithPendingStatus() {
        when(vehicleRepository.existsById(1L)).thenReturn(true);
        when(vehicleDocumentRepository.save(any(VehicleDocument.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        DocumentResponse response = vehicleDocumentService.create(1L, new DocumentRequest(
                DocumentType.INVOICE, "FV/2026/0001", LocalDate.of(2026, 11, 14), LocalDate.of(2027, 11, 13)));

        assertThat(response.vehicleId()).isEqualTo(1L);
        assertThat(response.type()).isEqualTo(DocumentType.INVOICE);
        assertThat(response.number()).isEqualTo("FV/2026/0001");
        assertThat(response.issueDate()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(response.expiryDate()).isEqualTo(LocalDate.of(2027, 11, 13));
        assertThat(response.status()).isEqualTo(DocumentStatus.PENDING);
    }

    // scenariusz 19
    @Test
    void createThrowsNotFoundWhenVehicleDoesNotExist() {
        when(vehicleRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> vehicleDocumentService.create(99L, new DocumentRequest(
                DocumentType.INVOICE, "FV/2026/0001", LocalDate.of(2026, 11, 14), null)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage(VEHICLE_NOT_FOUND);
        verify(vehicleDocumentRepository, never()).save(any());
    }

    private static VehicleDocument document(DocumentType type, String number) {
        return new VehicleDocument(
                1L, type, number, LocalDate.of(2026, 11, 14), null, DocumentStatus.PENDING);
    }
}
