package com.example.dealership.document;

import java.util.List;

import com.example.dealership.error.ConflictException;
import com.example.dealership.error.NotFoundException;
import com.example.dealership.vehicle.VehicleRepository;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class VehicleDocumentService {

    private final VehicleDocumentRepository vehicleDocumentRepository;
    private final VehicleRepository vehicleRepository;

    public VehicleDocumentService(VehicleDocumentRepository vehicleDocumentRepository,
            VehicleRepository vehicleRepository) {
        this.vehicleDocumentRepository = vehicleDocumentRepository;
        this.vehicleRepository = vehicleRepository;
    }

    public List<DocumentResponse> findAll(Long vehicleId) {
        checkVehicleExists(vehicleId);
        return vehicleDocumentRepository.findByVehicleId(vehicleId, Sort.by("id")).stream()
                .map(VehicleDocumentMapper::toResponse)
                .toList();
    }

    public DocumentResponse findById(Long vehicleId, Long documentId) {
        return VehicleDocumentMapper.toResponse(getDocument(vehicleId, documentId));
    }

    @Transactional
    public DocumentResponse create(Long vehicleId, DocumentRequest request) {
        checkVehicleExists(vehicleId);
        if (vehicleDocumentRepository.existsByVehicleIdAndTypeAndStatusNot(
                vehicleId, request.type(), DocumentStatus.REJECTED)) {
            throw new ConflictException(
                    "Vehicle with id " + vehicleId + " already has an active " + request.type() + " document");
        }
        VehicleDocument document = vehicleDocumentRepository.save(VehicleDocumentMapper.toEntity(vehicleId, request));
        return VehicleDocumentMapper.toResponse(document);
    }

    @Transactional
    public DocumentResponse changeStatus(Long vehicleId, Long documentId, DocumentStatusRequest request) {
        VehicleDocument document = getDocument(vehicleId, documentId);
        if (!document.getStatus().canChangeTo(request.status())) {
            throw new ConflictException(
                    "Cannot change document status from " + document.getStatus() + " to " + request.status());
        }
        document.setStatus(request.status());
        return VehicleDocumentMapper.toResponse(document);
    }

    private VehicleDocument getDocument(Long vehicleId, Long documentId) {
        checkVehicleExists(vehicleId);
        return vehicleDocumentRepository.findByIdAndVehicleId(documentId, vehicleId)
                .orElseThrow(() -> new NotFoundException("Document with id " + documentId + " not found"));
    }

    private void checkVehicleExists(Long vehicleId) {
        if (!vehicleRepository.existsById(vehicleId)) {
            throw new NotFoundException("Vehicle with id " + vehicleId + " not found");
        }
    }
}
