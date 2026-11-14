package com.example.dealership.document;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.List;

import com.example.dealership.error.NotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HandoverReadinessController.class)
class HandoverReadinessControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private HandoverReadinessService handoverReadinessService;

    // kontrakt BRIEF.md: dokładny kształt, bez dodatkowych pól, `documentType: null` jawnie w JSON
    @Test
    void returnsReadinessInContractShape() throws Exception {
        when(handoverReadinessService.check(1L)).thenReturn(new HandoverReadinessResponse(
                false,
                List.of(DocumentType.COC),
                Arrays.asList(
                        new ReadinessProblem(DocumentType.OC_POLICY, ReadinessReason.EXPIRED),
                        new ReadinessProblem(null, ReadinessReason.NO_HANDOVER_DATE))));

        mockMvc.perform(get("/api/vehicles/1/handover-readiness"))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "ready": false,
                          "missingDocumentTypes": ["COC"],
                          "problems": [
                            {"documentType": "OC_POLICY", "reason": "EXPIRED"},
                            {"documentType": null, "reason": "NO_HANDOVER_DATE"}
                          ]
                        }
                        """, true))
                .andExpect(jsonPath("$.problems[1].documentType").value(nullValue()));
    }

    // scenariusz 19
    @Test
    void returnsNotFoundProblemWhenVehicleMissing() throws Exception {
        when(handoverReadinessService.check(99L)).thenThrow(new NotFoundException("Vehicle with id 99 not found"));

        mockMvc.perform(get("/api/vehicles/99/handover-readiness"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Vehicle with id 99 not found"));
    }
}
