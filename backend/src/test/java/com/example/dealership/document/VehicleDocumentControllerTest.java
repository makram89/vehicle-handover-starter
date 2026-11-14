package com.example.dealership.document;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;

import com.example.dealership.error.NotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VehicleDocumentController.class)
class VehicleDocumentControllerTest {

    private static final String URL = "/api/vehicles/1/documents";
    private static final String VEHICLE_NOT_FOUND = "Vehicle with id 99 not found";
    private static final String DOCUMENT_NOT_FOUND = "Document with id 5 not found";
    private static final String VALID_BODY = """
            {
              "type": "INVOICE",
              "number": "FV/2026/0001",
              "issueDate": "2026-11-14",
              "expiryDate": "2027-11-13"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleDocumentService vehicleDocumentService;

    // A-Q5, A-Q6 (pełny zestaw pól)
    @Test
    void findAllReturnsDocuments() throws Exception {
        when(vehicleDocumentService.findAll(1L)).thenReturn(List.of(
                response(5L, DocumentType.INVOICE, "FV/2026/0001", LocalDate.of(2027, 11, 13), DocumentStatus.PENDING),
                response(6L, DocumentType.SALES_AGREEMENT, "UM/2026/0007", null, DocumentStatus.VERIFIED)));

        mockMvc.perform(get(URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(5))
                .andExpect(jsonPath("$[0].vehicleId").value(1))
                .andExpect(jsonPath("$[0].type").value("INVOICE"))
                .andExpect(jsonPath("$[0].number").value("FV/2026/0001"))
                .andExpect(jsonPath("$[0].issueDate").value("2026-11-14"))
                .andExpect(jsonPath("$[0].expiryDate").value("2027-11-13"))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[1].id").value(6))
                .andExpect(jsonPath("$[1].type").value("SALES_AGREEMENT"))
                .andExpect(jsonPath("$[1].expiryDate").value(nullValue()))
                .andExpect(jsonPath("$[1].status").value("VERIFIED"));
    }

    // scenariusz 19
    @Test
    void findAllReturnsNotFoundProblemWhenVehicleDoesNotExist() throws Exception {
        when(vehicleDocumentService.findAll(99L)).thenThrow(new NotFoundException(VEHICLE_NOT_FOUND));

        mockMvc.perform(get("/api/vehicles/99/documents"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(VEHICLE_NOT_FOUND));
    }

    @Test
    void findByIdReturnsDocument() throws Exception {
        when(vehicleDocumentService.findById(1L, 5L)).thenReturn(
                response(5L, DocumentType.INVOICE, "FV/2026/0001", LocalDate.of(2027, 11, 13), DocumentStatus.PENDING));

        mockMvc.perform(get(URL + "/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.vehicleId").value(1))
                .andExpect(jsonPath("$.number").value("FV/2026/0001"));
    }

    @Test
    void findByIdReturnsNotFoundProblemWhenDocumentDoesNotExist() throws Exception {
        when(vehicleDocumentService.findById(1L, 5L)).thenThrow(new NotFoundException(DOCUMENT_NOT_FOUND));

        mockMvc.perform(get(URL + "/5"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(DOCUMENT_NOT_FOUND));
    }

    // scenariusz 19
    @Test
    void findByIdReturnsNotFoundProblemWhenVehicleDoesNotExist() throws Exception {
        when(vehicleDocumentService.findById(99L, 5L)).thenThrow(new NotFoundException(VEHICLE_NOT_FOUND));

        mockMvc.perform(get("/api/vehicles/99/documents/5"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(VEHICLE_NOT_FOUND));
    }

    // A-R4, scenariusz 4
    @Test
    void createReturnsCreatedDocument() throws Exception {
        when(vehicleDocumentService.create(any(Long.class), any(DocumentRequest.class))).thenReturn(
                response(5L, DocumentType.INVOICE, "FV/2026/0001", LocalDate.of(2027, 11, 13), DocumentStatus.PENDING));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/vehicles/1/documents/5"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.vehicleId").value(1))
                .andExpect(jsonPath("$.status").value("PENDING"));
        verify(vehicleDocumentService).create(1L, new DocumentRequest(
                DocumentType.INVOICE, "FV/2026/0001", LocalDate.of(2026, 11, 14), LocalDate.of(2027, 11, 13)));
    }

    // A-R8, A-Q9
    @Test
    void createReturnsCreatedDocumentWhenExpiryDateIsMissing() throws Exception {
        String body = """
                {
                  "type": "OC_POLICY",
                  "number": "OC/2026/0042",
                  "issueDate": "2026-11-14"
                }
                """;
        when(vehicleDocumentService.create(any(Long.class), any(DocumentRequest.class))).thenReturn(
                response(6L, DocumentType.OC_POLICY, "OC/2026/0042", null, DocumentStatus.PENDING));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/vehicles/1/documents/6"))
                .andExpect(jsonPath("$.expiryDate").value(nullValue()));
        verify(vehicleDocumentService).create(1L, new DocumentRequest(
                DocumentType.OC_POLICY, "OC/2026/0042", LocalDate.of(2026, 11, 14), null));
    }

    // scenariusz 19
    @Test
    void createReturnsNotFoundProblemWhenVehicleDoesNotExist() throws Exception {
        when(vehicleDocumentService.create(any(Long.class), any(DocumentRequest.class)))
                .thenThrow(new NotFoundException(VEHICLE_NOT_FOUND));

        mockMvc.perform(post("/api/vehicles/99/documents").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(VEHICLE_NOT_FOUND));
    }

    // A-Q11
    @Test
    void createReturnsBadRequestProblemWithFieldErrorsWhenRequestIsInvalid() throws Exception {
        String body = """
                {
                  "number": "",
                  "expiryDate": "2027-11-13"
                }
                """;

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[0].field").value("issueDate"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be null"))
                .andExpect(jsonPath("$.errors[1].field").value("number"))
                .andExpect(jsonPath("$.errors[1].message").value("must not be blank"))
                .andExpect(jsonPath("$.errors[2].field").value("type"))
                .andExpect(jsonPath("$.errors[2].message").value("must not be null"));
        verifyNoInteractions(vehicleDocumentService);
    }

    // A-Q11 (granica 50 znaków)
    @Test
    void createReturnsBadRequestProblemWhenNumberIsTooLong() throws Exception {
        String body = VALID_BODY.replace("FV/2026/0001", "N".repeat(51));

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("number"))
                .andExpect(jsonPath("$.errors[0].message").value("size must be between 0 and 50"));
        verifyNoInteractions(vehicleDocumentService);
    }

    @Test
    void createReturnsEnglishValidationMessagesWhenAcceptLanguageIsPolish() throws Exception {
        String body = VALID_BODY.replace("FV/2026/0001", " ");

        mockMvc.perform(post(URL)
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "pl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("number"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
        verifyNoInteractions(vehicleDocumentService);
    }

    @Test
    void createReturnsBadRequestProblemWithFieldErrorWhenTypeIsUnknown() throws Exception {
        String body = VALID_BODY.replace("INVOICE", "RECEIPT");

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("type"))
                .andExpect(jsonPath("$.errors[0].message")
                        .value("must be one of: SALES_AGREEMENT, INVOICE, COC, OC_POLICY, REGISTRATION_POA"));
        verifyNoInteractions(vehicleDocumentService);
    }

    @Test
    void createReturnsBadRequestProblemWithFieldErrorWhenDateHasWrongType() throws Exception {
        String body = VALID_BODY.replace("2026-11-14", "yesterday");

        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("issueDate"))
                .andExpect(jsonPath("$.errors[0].message").value("has an invalid value"));
        verifyNoInteractions(vehicleDocumentService);
    }

    @Test
    void createReturnsBadRequestProblemWhenBodyIsNotJson() throws Exception {
        mockMvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value(nullValue()))
                .andExpect(jsonPath("$.errors[0].message").value("request body must be valid JSON"));
        verifyNoInteractions(vehicleDocumentService);
    }

    private static DocumentResponse response(
            Long id, DocumentType type, String number, LocalDate expiryDate, DocumentStatus status) {
        return new DocumentResponse(id, 1L, type, number, LocalDate.of(2026, 11, 14), expiryDate, status);
    }
}
