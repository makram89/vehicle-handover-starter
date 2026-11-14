package com.example.dealership.document;

public final class VehicleDocumentMapper {

    private VehicleDocumentMapper() {
    }

    public static VehicleDocument toEntity(Long vehicleId, DocumentRequest request) {
        return new VehicleDocument(
                vehicleId,
                request.type(),
                request.number(),
                request.issueDate(),
                request.expiryDate(),
                DocumentStatus.PENDING);
    }

    public static DocumentResponse toResponse(VehicleDocument document) {
        return new DocumentResponse(
                document.getId(),
                document.getVehicleId(),
                document.getType(),
                document.getNumber(),
                document.getIssueDate(),
                document.getExpiryDate(),
                document.getStatus());
    }
}
