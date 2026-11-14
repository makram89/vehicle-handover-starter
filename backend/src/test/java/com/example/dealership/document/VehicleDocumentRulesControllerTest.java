package com.example.dealership.document;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.example.dealership.error.ConflictException;
import com.example.dealership.error.NotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VehicleDocumentController.class)
@Import(FixedClockTestConfig.class)
class VehicleDocumentRulesControllerTest {

    private static final String URL = "/api/vehicles/1/documents";
    private static final String STATUS_URL = URL + "/5/status";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleDocumentService vehicleDocumentService;

    // A-R10, scenariusz 4 (granica: dziś jest dozwolone)
    @Test
    void createAcceptsIssueDateToday() throws Exception {
        when(vehicleDocumentService.create(any(Long.class), any(DocumentRequest.class))).thenReturn(response(null));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("2026-11-14", "null")))
                .andExpect(status().isCreated());
        verify(vehicleDocumentService).create(1L, new DocumentRequest(
                DocumentType.INVOICE, "FV/2026/0001", LocalDate.of(2026, 11, 14), null));
    }

    // A-R10, scenariusz 5
    @Test
    void createRejectsIssueDateInFuture() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("2026-11-15", "null")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("issueDate"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be in the future"));
        verifyNoInteractions(vehicleDocumentService);
    }

    // A-Q10, scenariusz 18
    @Test
    void createRejectsExpiryDateBeforeIssueDate() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-11-14", "\"2026-11-13\"")))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("expiryDate"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be before issue date"));
        verifyNoInteractions(vehicleDocumentService);
    }

    // A-Q10 (granica: równe daty są dozwolone)
    @Test
    void createAcceptsExpiryDateEqualToIssueDate() throws Exception {
        when(vehicleDocumentService.create(any(Long.class), any(DocumentRequest.class)))
                .thenReturn(response(LocalDate.of(2026, 11, 14)));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON)
                        .content(body("2026-11-14", "\"2026-11-14\"")))
                .andExpect(status().isCreated());
    }

    // A-R8
    @Test
    void createAcceptsMissingExpiryDate() throws Exception {
        when(vehicleDocumentService.create(any(Long.class), any(DocumentRequest.class))).thenReturn(response(null));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("""
                        {"type": "INVOICE", "number": "FV/2026/0001", "issueDate": "2026-11-14"}
                        """))
                .andExpect(status().isCreated());
    }

    // A-R11, scenariusz 9
    @Test
    void createReturnsConflictProblemWhenActiveDocumentExists() throws Exception {
        when(vehicleDocumentService.create(any(Long.class), any(DocumentRequest.class)))
                .thenThrow(new ConflictException("Vehicle with id 1 already has an active INVOICE document"));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body("2026-11-14", "null")))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Vehicle with id 1 already has an active INVOICE document"));
    }

    // A-R5, scenariusz 6
    @Test
    void changeStatusReturnsUpdatedDocument() throws Exception {
        when(vehicleDocumentService.changeStatus(1L, 5L, new DocumentStatusRequest(DocumentStatus.VERIFIED)))
                .thenReturn(new DocumentResponse(5L, 1L, DocumentType.INVOICE, "FV/2026/0001",
                        LocalDate.of(2026, 11, 14), null, DocumentStatus.VERIFIED));

        mockMvc.perform(put(STATUS_URL).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VERIFIED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.vehicleId").value(1))
                .andExpect(jsonPath("$.type").value("INVOICE"))
                .andExpect(jsonPath("$.status").value("VERIFIED"));
    }

    // A-R7, scenariusz 7
    @Test
    void changeStatusReturnsConflictProblemForForbiddenTransition() throws Exception {
        when(vehicleDocumentService.changeStatus(1L, 5L, new DocumentStatusRequest(DocumentStatus.VERIFIED)))
                .thenThrow(new ConflictException("Cannot change document status from REJECTED to VERIFIED"));

        mockMvc.perform(put(STATUS_URL).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"VERIFIED\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Cannot change document status from REJECTED to VERIFIED"));
    }

    @Test
    void changeStatusRejectsMissingStatus() throws Exception {
        mockMvc.perform(put(STATUS_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("status"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be null"));
        verifyNoInteractions(vehicleDocumentService);
    }

    @Test
    void changeStatusRejectsUnknownStatus() throws Exception {
        mockMvc.perform(put(STATUS_URL).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"ARCHIVED\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("status"))
                .andExpect(jsonPath("$.errors[0].message").value("must be one of: PENDING, VERIFIED, REJECTED"));
        verifyNoInteractions(vehicleDocumentService);
    }

    // scenariusz 19
    @Test
    void changeStatusReturnsNotFoundProblemWhenVehicleMissing() throws Exception {
        when(vehicleDocumentService.changeStatus(99L, 5L, new DocumentStatusRequest(DocumentStatus.VERIFIED)))
                .thenThrow(new NotFoundException("Vehicle with id 99 not found"));

        mockMvc.perform(put("/api/vehicles/99/documents/5/status").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"VERIFIED\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Vehicle with id 99 not found"));
    }

    private static String body(String issueDate, String expiryDateJson) {
        return """
                {
                  "type": "INVOICE",
                  "number": "FV/2026/0001",
                  "issueDate": "%s",
                  "expiryDate": %s
                }
                """.formatted(issueDate, expiryDateJson);
    }

    private static DocumentResponse response(LocalDate expiryDate) {
        return new DocumentResponse(5L, 1L, DocumentType.INVOICE, "FV/2026/0001",
                LocalDate.of(2026, 11, 14), expiryDate, DocumentStatus.PENDING);
    }
}
