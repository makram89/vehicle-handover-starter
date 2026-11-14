package com.example.dealership.customer;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.example.dealership.error.ConflictException;
import com.example.dealership.error.NotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    private static final String EMAIL = "jan@example.com";
    private static final String VALID_BODY = """
            {
              "firstName": "Jan",
              "lastName": "Kowalski",
              "email": "jan@example.com",
              "phone": "+48 600 100 200",
              "nip": "1234567890"
            }
            """;
    private static final String BODY_WITHOUT_NIP = """
            {
              "firstName": "Jan",
              "lastName": "Kowalski",
              "email": "jan@example.com",
              "phone": "+48 600 100 200"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    // B-R1, B-Q5 (pełny zestaw pól, brak companyName/typu klienta)
    @Test
    void findAllReturnsCustomers() throws Exception {
        when(customerService.findAll()).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].firstName").value("Jan"))
                .andExpect(jsonPath("$[0].lastName").value("Kowalski"))
                .andExpect(jsonPath("$[0].email").value(EMAIL))
                .andExpect(jsonPath("$[0].phone").value("+48 600 100 200"))
                .andExpect(jsonPath("$[0].nip").value("1234567890"))
                .andExpect(jsonPath("$[0].length()").value(6));
    }

    // B-R1
    @Test
    void findByIdReturnsCustomer() throws Exception {
        when(customerService.findById(1L)).thenReturn(response());

        mockMvc.perform(get("/api/customers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    // scenariusz 7
    @Test
    void findByIdReturnsNotFoundProblemWhenCustomerDoesNotExist() throws Exception {
        when(customerService.findById(99L)).thenThrow(new NotFoundException("Customer with id 99 not found"));

        mockMvc.perform(get("/api/customers/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Customer with id 99 not found"));
    }

    // B-R1
    @Test
    void createReturnsCreatedCustomer() throws Exception {
        when(customerService.create(any(CustomerRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/customers/1"))
                .andExpect(jsonPath("$.id").value(1));
    }

    // B-R3, B-Q1, scenariusz 1
    @Test
    void createReturnsCreatedCustomerWhenNipIsMissing() throws Exception {
        when(customerService.create(any(CustomerRequest.class))).thenReturn(responseWithoutNip());

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(BODY_WITHOUT_NIP))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/customers/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nip").value(nullValue()));
    }

    // B-R2, B-Q1, scenariusz 5, S1
    @Test
    void createReturnsBadRequestProblemWithFieldErrorsWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(invalidBody()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(4))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"))
                .andExpect(jsonPath("$.errors[1].field").value("firstName"))
                .andExpect(jsonPath("$.errors[1].message").value("must not be blank"))
                .andExpect(jsonPath("$.errors[2].field").value("lastName"))
                .andExpect(jsonPath("$.errors[2].message").value("must not be blank"))
                .andExpect(jsonPath("$.errors[3].field").value("phone"))
                .andExpect(jsonPath("$.errors[3].message").value("must not be blank"));
        verifyNoInteractions(customerService);
    }

    // B-R2, B-Q2, scenariusz 6
    @Test
    void createReturnsBadRequestProblemWithFieldErrorsWhenFormatsAreInvalid() throws Exception {
        String body = VALID_BODY
                .replace("jan@example.com", "x")
                .replace("1234567890", "123")
                .replace("+48 600 100 200", "abc");

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(3))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("must be a well-formed email address"))
                .andExpect(jsonPath("$.errors[1].field").value("nip"))
                .andExpect(jsonPath("$.errors[1].message").value("must be 10 digits"))
                .andExpect(jsonPath("$.errors[2].field").value("phone"))
                .andExpect(jsonPath("$.errors[2].message").value(
                        "must be 9 to 20 characters: digits, spaces, hyphens, optional leading +, "
                                + "starting and ending with a digit"));
        verifyNoInteractions(customerService);
    }

    @Test
    void createReturnsEnglishValidationMessagesWhenAcceptLanguageIsPolish() throws Exception {
        String body = VALID_BODY.replace("Jan", " ");

        mockMvc.perform(post("/api/customers")
                        .header(HttpHeaders.ACCEPT_LANGUAGE, "pl")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("firstName"))
                .andExpect(jsonPath("$.errors[0].message").value("must not be blank"));
        verifyNoInteractions(customerService);
    }

    @Test
    void createReturnsBadRequestProblemWhenBodyIsNotJson() throws Exception {
        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value(nullValue()))
                .andExpect(jsonPath("$.errors[0].message").value("request body must be valid JSON"));
        verifyNoInteractions(customerService);
    }

    // B-R4, scenariusz 2
    @Test
    void createReturnsConflictProblemWhenEmailAlreadyExists() throws Exception {
        when(customerService.create(any(CustomerRequest.class)))
                .thenThrow(new ConflictException("Customer with email " + EMAIL + " already exists"));

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Customer with email " + EMAIL + " already exists"));
    }

    // B-R4
    @Test
    void createReturnsConflictProblemWhenDatabaseConstraintIsViolated() throws Exception {
        when(customerService.create(any(CustomerRequest.class)))
                .thenThrow(new DataIntegrityViolationException("uq_customer_email"));

        mockMvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Request conflicts with existing data"));
    }

    // B-R1, scenariusz 3
    @Test
    void updateReturnsUpdatedCustomer() throws Exception {
        when(customerService.update(eq(1L), any(CustomerRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/customers/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(EMAIL));
    }

    // scenariusz 7
    @Test
    void updateReturnsNotFoundProblemWhenCustomerDoesNotExist() throws Exception {
        when(customerService.update(eq(99L), any(CustomerRequest.class)))
                .thenThrow(new NotFoundException("Customer with id 99 not found"));

        mockMvc.perform(put("/api/customers/99").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Customer with id 99 not found"));
    }

    // scenariusz 5 (wariant PUT)
    @Test
    void updateReturnsBadRequestProblemWithFieldErrorsWhenRequestIsInvalid() throws Exception {
        mockMvc.perform(put("/api/customers/1").contentType(MediaType.APPLICATION_JSON).content(invalidBody()))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Validation failed"))
                .andExpect(jsonPath("$.errors.length()").value(4))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[1].field").value("firstName"))
                .andExpect(jsonPath("$.errors[2].field").value("lastName"))
                .andExpect(jsonPath("$.errors[3].field").value("phone"));
        verifyNoInteractions(customerService);
    }

    // B-R4, scenariusz 4
    @Test
    void updateReturnsConflictProblemWhenEmailAlreadyExists() throws Exception {
        when(customerService.update(eq(1L), any(CustomerRequest.class)))
                .thenThrow(new ConflictException("Customer with email " + EMAIL + " already exists"));

        mockMvc.perform(put("/api/customers/1").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Customer with email " + EMAIL + " already exists"));
    }

    // B-R1, scenariusz 8
    @Test
    void deleteReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/customers/1"))
                .andExpect(status().isNoContent());

        verify(customerService).delete(1L);
    }

    // scenariusz 7
    @Test
    void deleteReturnsNotFoundProblemWhenCustomerDoesNotExist() throws Exception {
        doThrow(new NotFoundException("Customer with id 99 not found")).when(customerService).delete(99L);

        mockMvc.perform(delete("/api/customers/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Customer with id 99 not found"));
    }

    private static String invalidBody() {
        return """
                {
                  "firstName": "",
                  "lastName": "",
                  "email": "",
                  "nip": "1234567890"
                }
                """;
    }

    private static CustomerResponse response() {
        return new CustomerResponse(1L, "Jan", "Kowalski", EMAIL, "+48 600 100 200", "1234567890");
    }

    private static CustomerResponse responseWithoutNip() {
        return new CustomerResponse(1L, "Jan", "Kowalski", EMAIL, "+48 600 100 200", null);
    }
}
