package com.example.dealership.document;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// Prawdziwe H2 + Flyway, stały Clock (dziś = 2026-11-14). Baza jest wspólna dla testów klasy,
// więc każdy test tworzy własny pojazd z unikalnym VIN (inny niż w seedzie).
@SpringBootTest
@AutoConfigureMockMvc
@Import(FixedClockTestConfig.class)
class VehicleDocumentFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // A-R3, A-R2, scenariusze 2 i 3
    @Test
    void registrationByDealerCanBeSetAndIsFalseForNewVehicle() throws Exception {
        int vehicleId = createVehicle("ZTDCFLW0000000001", "2026-12-15");

        mockMvc.perform(get("/api/vehicles/" + vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationByDealer").value(false));

        mockMvc.perform(put("/api/vehicles/" + vehicleId + "/registration-by-dealer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationByDealer\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(vehicleId))
                .andExpect(jsonPath("$.vin").value("ZTDCFLW0000000001"))
                .andExpect(jsonPath("$.registrationByDealer").value(true));

        mockMvc.perform(get("/api/vehicles/" + vehicleId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationByDealer").value(true));

        mockMvc.perform(get("/api/vehicles/" + vehicleId + "/handover-readiness"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "ready": false,
                          "missingDocumentTypes": ["SALES_AGREEMENT", "INVOICE", "COC", "OC_POLICY",
                                                   "REGISTRATION_POA"],
                          "problems": []
                        }
                        """, true));
    }

    // A-R11, A-R12, scenariusze 9 i 10
    @Test
    void rejectedDocumentCanBeReplacedAndBothAreListed() throws Exception {
        int vehicleId = createVehicle("ZTDCFLW0000000002", "2026-12-15");
        int firstId = createDocument(vehicleId, "INVOICE", "FV/1", "2026-11-14", null);

        mockMvc.perform(put(statusUrl(vehicleId, firstId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"REJECTED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(post(documentsUrl(vehicleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentBody("INVOICE", "FV/2", "2026-11-14", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get(documentsUrl(vehicleId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].number").value("FV/1"))
                .andExpect(jsonPath("$[0].status").value("REJECTED"))
                .andExpect(jsonPath("$[1].number").value("FV/2"))
                .andExpect(jsonPath("$[1].status").value("PENDING"));

        mockMvc.perform(post(documentsUrl(vehicleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentBody("INVOICE", "FV/3", "2026-11-14", null)))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail")
                        .value("Vehicle with id " + vehicleId + " already has an active INVOICE document"));
    }

    // A-R7, scenariusz 7 (przez API, ze stanem w bazie)
    @Test
    void rejectedDocumentStaysRejected() throws Exception {
        int vehicleId = createVehicle("ZTDCFLW0000000006", "2026-12-15");
        int documentId = createDocument(vehicleId, "COC", "COC/1", "2026-11-14", null);
        mockMvc.perform(put(statusUrl(vehicleId, documentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"REJECTED\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put(statusUrl(vehicleId, documentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"VERIFIED\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Cannot change document status from REJECTED to VERIFIED"));

        mockMvc.perform(get(documentsUrl(vehicleId) + "/" + documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    // A-R10 z prawdziwym beanem Clock w walidatorze (dziś = 2026-11-14), scenariusz 5
    @Test
    void createRejectsIssueDateInFutureAccordingToClock() throws Exception {
        int vehicleId = createVehicle("ZTDCFLW0000000007", "2026-12-15");

        mockMvc.perform(post(documentsUrl(vehicleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentBody("INVOICE", "FV/1", "2026-11-15", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("issueDate"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be in the future"));

        mockMvc.perform(post(documentsUrl(vehicleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentBody("INVOICE", "FV/1", "2026-11-14", null)))
                .andExpect(status().isCreated());
    }

    // A-R14, A-Q4, scenariusz 16 (przez API)
    @Test
    void readinessReportsExpiredAndNoHandoverDateWhenHandoverDateMissing() throws Exception {
        int vehicleId = createVehicle("ZTDCFLW0000000003", null);
        verify(vehicleId, createDocument(vehicleId, "SALES_AGREEMENT", "UM/1", "2026-01-15", null));
        verify(vehicleId, createDocument(vehicleId, "INVOICE", "FV/1", "2026-01-15", null));
        verify(vehicleId, createDocument(vehicleId, "COC", "COC/1", "2026-01-15", null));
        verify(vehicleId, createDocument(vehicleId, "OC_POLICY", "OC/1", "2026-01-15", "2026-11-13"));

        mockMvc.perform(get("/api/vehicles/" + vehicleId + "/handover-readiness"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "ready": false,
                          "missingDocumentTypes": [],
                          "problems": [
                            {"documentType": "OC_POLICY", "reason": "EXPIRED"},
                            {"documentType": null, "reason": "NO_HANDOVER_DATE"}
                          ]
                        }
                        """, true));
    }

    // A-Q12, kaskada (usunięcie pojazdu nadal 204)
    @Test
    void deletingVehicleCascadesToDocuments() throws Exception {
        int vehicleId = createVehicle("ZTDCFLW0000000004", "2026-12-15");
        createDocument(vehicleId, "INVOICE", "FV/1", "2026-11-14", null);

        mockMvc.perform(delete("/api/vehicles/" + vehicleId)).andExpect(status().isNoContent());

        mockMvc.perform(get(documentsUrl(vehicleId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Vehicle with id " + vehicleId + " not found"));
    }

    // scenariusz 19
    @Test
    void readinessReturnsNotFoundForMissingVehicle() throws Exception {
        mockMvc.perform(get("/api/vehicles/99999/handover-readiness"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Vehicle with id 99999 not found"));
    }

    private int createVehicle(String vin, String plannedHandoverDate) throws Exception {
        String handover = plannedHandoverDate == null ? "null" : "\"" + plannedHandoverDate + "\"";
        String body = """
                {
                  "vin": "%s",
                  "make": "Skoda",
                  "model": "Fabia",
                  "productionYear": 2025,
                  "fuelType": "PETROL",
                  "plannedHandoverDate": %s
                }
                """.formatted(vin, handover);
        String response = mockMvc.perform(post("/api/vehicles").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private int createDocument(int vehicleId, String type, String number, String issueDate, String expiryDate)
            throws Exception {
        String response = mockMvc.perform(post(documentsUrl(vehicleId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(documentBody(type, number, issueDate, expiryDate)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private void verify(int vehicleId, int documentId) throws Exception {
        mockMvc.perform(put(statusUrl(vehicleId, documentId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"VERIFIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    private static String documentsUrl(int vehicleId) {
        return "/api/vehicles/" + vehicleId + "/documents";
    }

    private static String statusUrl(int vehicleId, int documentId) {
        return documentsUrl(vehicleId) + "/" + documentId + "/status";
    }

    private static String documentBody(String type, String number, String issueDate, String expiryDate) {
        String expiry = expiryDate == null ? "null" : "\"" + expiryDate + "\"";
        return """
                {
                  "type": "%s",
                  "number": "%s",
                  "issueDate": "%s",
                  "expiryDate": %s
                }
                """.formatted(type, number, issueDate, expiry);
    }
}
