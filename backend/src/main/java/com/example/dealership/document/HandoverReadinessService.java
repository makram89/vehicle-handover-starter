package com.example.dealership.document;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.example.dealership.error.NotFoundException;
import com.example.dealership.vehicle.Vehicle;
import com.example.dealership.vehicle.VehicleRepository;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class HandoverReadinessService {

    private static final List<DocumentType> ALWAYS_REQUIRED = List.of(
            DocumentType.SALES_AGREEMENT, DocumentType.INVOICE, DocumentType.COC, DocumentType.OC_POLICY);

    private final VehicleRepository vehicleRepository;
    private final VehicleDocumentRepository vehicleDocumentRepository;
    private final Clock clock;

    public HandoverReadinessService(VehicleRepository vehicleRepository,
            VehicleDocumentRepository vehicleDocumentRepository, Clock clock) {
        this.vehicleRepository = vehicleRepository;
        this.vehicleDocumentRepository = vehicleDocumentRepository;
        this.clock = clock;
    }

    public HandoverReadinessResponse check(Long vehicleId) {
        Vehicle vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle with id " + vehicleId + " not found"));
        List<VehicleDocument> documents = vehicleDocumentRepository.findByVehicleId(vehicleId, Sort.by("id"));

        LocalDate handoverDate = vehicle.getPlannedHandoverDate();
        LocalDate referenceDate = handoverDate != null ? handoverDate : LocalDate.now(clock);

        List<DocumentType> missing = new ArrayList<>();
        List<ReadinessProblem> problems = new ArrayList<>();
        for (DocumentType type : DocumentType.values()) {
            if (!isRequired(type, vehicle)) {
                continue;
            }
            Optional<VehicleDocument> active = documents.stream()
                    .filter(document -> document.getType() == type && document.getStatus() != DocumentStatus.REJECTED)
                    .findFirst();
            if (active.isEmpty()) {
                missing.add(type);
                continue;
            }
            VehicleDocument document = active.get();
            if (document.getStatus() == DocumentStatus.PENDING) {
                problems.add(new ReadinessProblem(type, ReadinessReason.NOT_VERIFIED));
            }
            if (document.getExpiryDate() != null && document.getExpiryDate().isBefore(referenceDate)) {
                problems.add(new ReadinessProblem(type, ReadinessReason.EXPIRED));
            }
        }
        if (handoverDate == null) {
            problems.add(new ReadinessProblem(null, ReadinessReason.NO_HANDOVER_DATE));
        }
        return new HandoverReadinessResponse(missing.isEmpty() && problems.isEmpty(), missing, problems);
    }

    private static boolean isRequired(DocumentType type, Vehicle vehicle) {
        return ALWAYS_REQUIRED.contains(type)
                || (type == DocumentType.REGISTRATION_POA && vehicle.isRegistrationByDealer());
    }
}
