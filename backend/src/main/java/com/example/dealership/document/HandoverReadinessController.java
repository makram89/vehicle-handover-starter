package com.example.dealership.document;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicles/{vehicleId}/handover-readiness")
public class HandoverReadinessController {

    private final HandoverReadinessService handoverReadinessService;

    public HandoverReadinessController(HandoverReadinessService handoverReadinessService) {
        this.handoverReadinessService = handoverReadinessService;
    }

    @GetMapping
    public HandoverReadinessResponse check(@PathVariable Long vehicleId) {
        return handoverReadinessService.check(vehicleId);
    }
}
