package com.example.dealership.vehicle;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import com.example.dealership.error.NotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(VehicleController.class)
class VehicleRegistrationByDealerControllerTest {

    private static final String URL = "/api/vehicles/1/registration-by-dealer";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VehicleService vehicleService;

    // A-R3, scenariusz 3
    @Test
    void returnsVehicleWithUpdatedFlag() throws Exception {
        when(vehicleService.updateRegistrationByDealer(1L, new RegistrationByDealerRequest(true)))
                .thenReturn(response(true));

        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{\"registrationByDealer\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.vin").value("WVWZZZ1KZAW000001"))
                .andExpect(jsonPath("$.make").value("Volkswagen"))
                .andExpect(jsonPath("$.model").value("Golf"))
                .andExpect(jsonPath("$.productionYear").value(2025))
                .andExpect(jsonPath("$.fuelType").value("PETROL"))
                .andExpect(jsonPath("$.plannedHandoverDate").value("2026-12-15"))
                .andExpect(jsonPath("$.registrationByDealer").value(true));
    }

    @Test
    void rejectsMissingFlag() throws Exception {
        mockMvc.perform(put(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("registrationByDealer"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be null"));
        verifyNoInteractions(vehicleService);
    }

    // scenariusz 19
    @Test
    void returnsNotFoundProblemWhenVehicleMissing() throws Exception {
        when(vehicleService.updateRegistrationByDealer(99L, new RegistrationByDealerRequest(true)))
                .thenThrow(new NotFoundException("Vehicle with id 99 not found"));

        mockMvc.perform(put("/api/vehicles/99/registration-by-dealer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"registrationByDealer\": true}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Vehicle with id 99 not found"));
    }

    // A-R3 (flaga widoczna w GET pojazdu)
    @Test
    void getVehicleIncludesRegistrationByDealer() throws Exception {
        when(vehicleService.findById(1L)).thenReturn(response(false));

        mockMvc.perform(get("/api/vehicles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationByDealer").value(false));
    }

    private static VehicleResponse response(boolean registrationByDealer) {
        return new VehicleResponse(1L, "WVWZZZ1KZAW000001", "Volkswagen", "Golf", 2025, FuelType.PETROL,
                LocalDate.of(2026, 12, 15), registrationByDealer);
    }
}
