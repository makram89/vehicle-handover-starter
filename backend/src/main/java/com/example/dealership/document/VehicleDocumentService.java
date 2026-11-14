package com.example.dealership.document;

import java.util.List;

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
        VehicleDocument document = vehicleDocumentRepository.save(VehicleDocumentMapper.toEntity(vehicleId, request));
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
