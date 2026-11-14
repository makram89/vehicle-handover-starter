package com.example.dealership.document;

public enum DocumentStatus {
    PENDING,
    VERIFIED,
    REJECTED;

    // Allowed transitions (INTENT_A.md section 4): PENDING -> VERIFIED | REJECTED, VERIFIED -> REJECTED.
    public boolean canChangeTo(DocumentStatus target) {
        return switch (this) {
            case PENDING -> target == VERIFIED || target == REJECTED;
            case VERIFIED -> target == REJECTED;
            case REJECTED -> false;
        };
    }
}
