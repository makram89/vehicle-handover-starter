package com.example.dealership.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.example.dealership.error.ConflictException;
import com.example.dealership.error.NotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    private static final String EMAIL = "jan@example.com";
    private static final String OTHER_EMAIL = "anna@example.com";

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    // B-R1
    @Test
    void findAllReturnsMappedCustomers() {
        when(customerRepository.findAll(Sort.by("id"))).thenReturn(List.of(customer(EMAIL), customer(OTHER_EMAIL)));

        List<CustomerResponse> responses = customerService.findAll();

        assertThat(responses).extracting(CustomerResponse::email).containsExactly(EMAIL, OTHER_EMAIL);
    }

    // scenariusz 7
    @Test
    void findByIdThrowsNotFoundWhenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Customer with id 99 not found");
    }

    // B-R1, B-Q5 (pełny zestaw pól)
    @Test
    void createSavesCustomer() {
        when(customerRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CustomerResponse response = customerService.create(request(EMAIL));

        assertThat(response.firstName()).isEqualTo("Jan");
        assertThat(response.lastName()).isEqualTo("Kowalski");
        assertThat(response.email()).isEqualTo(EMAIL);
        assertThat(response.phone()).isEqualTo("+48 600 100 200");
        assertThat(response.nip()).isEqualTo("1234567890");
    }

    // B-R4, scenariusz 2
    @Test
    void createThrowsConflictWhenEmailAlreadyExists() {
        when(customerRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> customerService.create(request(EMAIL)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Customer with email " + EMAIL + " already exists");
        verify(customerRepository, never()).save(any());
    }

    // B-R1, scenariusz 3
    @Test
    void updateChangesExistingCustomer() {
        Customer customer = customer(EMAIL);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.existsByEmailAndIdNot(OTHER_EMAIL, 1L)).thenReturn(false);
        when(customerRepository.save(customer)).thenReturn(customer);

        CustomerResponse response = customerService.update(1L, request(OTHER_EMAIL));

        assertThat(response.email()).isEqualTo(OTHER_EMAIL);
        assertThat(customer.getEmail()).isEqualTo(OTHER_EMAIL);
        verify(customerRepository).save(customer);
    }

    // B-R4, scenariusz 4
    @Test
    void updateThrowsConflictWhenEmailBelongsToAnotherCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer(EMAIL)));
        when(customerRepository.existsByEmailAndIdNot(OTHER_EMAIL, 1L)).thenReturn(true);

        assertThatThrownBy(() -> customerService.update(1L, request(OTHER_EMAIL)))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Customer with email " + OTHER_EMAIL + " already exists");
        verify(customerRepository, never()).save(any());
    }

    // scenariusz 7
    @Test
    void updateThrowsNotFoundWhenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.update(99L, request(EMAIL)))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Customer with id 99 not found");
    }

    // B-R1, scenariusz 8
    @Test
    void deleteRemovesExistingCustomer() {
        Customer customer = customer(EMAIL);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.delete(1L);

        verify(customerRepository).delete(customer);
    }

    // scenariusz 7
    @Test
    void deleteThrowsNotFoundWhenCustomerDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.delete(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Customer with id 99 not found");
    }

    private static Customer customer(String email) {
        return new Customer("Jan", "Kowalski", email, "+48 600 100 200", "1234567890");
    }

    private static CustomerRequest request(String email) {
        return new CustomerRequest("Jan", "Kowalski", email, "+48 600 100 200", "1234567890");
    }
}
