package com.example.dealership.document;

public record ReadinessProblem(
        DocumentType documentType,
        ReadinessReason reason
) {
}
