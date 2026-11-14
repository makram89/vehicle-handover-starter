package com.example.dealership.document;

import java.util.List;

public record HandoverReadinessResponse(
        boolean ready,
        List<DocumentType> missingDocumentTypes,
        List<ReadinessProblem> problems
) {
}
