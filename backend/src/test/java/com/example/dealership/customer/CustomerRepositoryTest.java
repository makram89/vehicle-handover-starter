package com.example.dealership.customer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class CustomerRepositoryTest {

    private static final String EMAIL = "jan@example.com";
    private static final String OTHER_EMAIL = "anna@example.com";

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private TestEntityManager entityManager;

    // B-R4, scenariusz 3
    @Test
    void existsByEmailAndIdNotFindsEmailOfAnotherCustomer() {
        Customer first = customerRepository.save(
                new Customer("Jan", "Kowalski", EMAIL, "+48 600 100 200", null));
        Customer second = customerRepository.save(
                new Customer("Anna", "Nowak", OTHER_EMAIL, "600-100-201", null));
        entityManager.flush();

        assertThat(customerRepository.existsByEmailAndIdNot(EMAIL, second.getId())).isTrue();
        assertThat(customerRepository.existsByEmailAndIdNot(EMAIL, first.getId())).isFalse();
        assertThat(customerRepository.existsByEmail("ewa@example.com")).isFalse();
    }

    // B-R1, B-R2
    @Test
    void saveStoresCustomerThatCanBeReadBack() {
        Customer saved = customerRepository.save(
                new Customer("Jan", "Kowalski", EMAIL, "+48 600 100 200", "1234567890"));
        entityManager.flush();
        entityManager.clear();

        Customer found = customerRepository.findById(saved.getId()).orElseThrow();

        assertThat(found).isNotSameAs(saved);
        assertThat(found.getFirstName()).isEqualTo("Jan");
        assertThat(found.getLastName()).isEqualTo("Kowalski");
        assertThat(found.getEmail()).isEqualTo(EMAIL);
        assertThat(found.getPhone()).isEqualTo("+48 600 100 200");
        assertThat(found.getNip()).isEqualTo("1234567890");
        assertThat(customerRepository.existsByEmail(EMAIL)).isTrue();
        assertThat(customerRepository.existsByEmailAndIdNot(EMAIL, saved.getId())).isFalse();
    }

    // B-R3, scenariusz 1
    @Test
    void saveStoresCustomerWithoutNip() {
        Customer saved = customerRepository.save(
                new Customer("Jan", "Kowalski", EMAIL, "+48 600 100 200", null));
        entityManager.flush();
        entityManager.clear();

        Customer found = customerRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getNip()).isNull();
    }

    // checklist B: tekst o długości max kolumny (S6: 50/50/100/25/10)
    @Test
    void saveStoresValuesOfMaximumColumnLength() {
        String firstName = "f".repeat(50);
        String lastName = "l".repeat(50);
        String email = "e".repeat(88) + "@example.com";
        String phone = "+" + "1".repeat(24);
        String nip = "1".repeat(10);
        Customer saved = customerRepository.save(new Customer(firstName, lastName, email, phone, nip));
        entityManager.flush();
        entityManager.clear();

        Customer found = customerRepository.findById(saved.getId()).orElseThrow();

        assertThat(email).hasSize(100);
        assertThat(found.getFirstName()).isEqualTo(firstName);
        assertThat(found.getLastName()).isEqualTo(lastName);
        assertThat(found.getEmail()).isEqualTo(email);
        assertThat(found.getPhone()).isEqualTo(phone);
        assertThat(found.getNip()).isEqualTo(nip);
    }

    // B-R4 (constraint uq_customer_email)
    @Test
    void saveRejectsDuplicateEmail() {
        customerRepository.saveAndFlush(new Customer("Jan", "Kowalski", EMAIL, "+48 600 100 200", null));

        assertThatThrownBy(() -> customerRepository.saveAndFlush(
                new Customer("Anna", "Nowak", EMAIL, "600-100-201", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // B-Q3
    @Test
    void existsByEmailIsCaseSensitive() {
        customerRepository.save(new Customer("Jan", "Kowalski", "Jan@Example.com", "+48 600 100 200", null));
        entityManager.flush();

        assertThat(customerRepository.existsByEmail("Jan@Example.com")).isTrue();
        assertThat(customerRepository.existsByEmail("jan@example.com")).isFalse();
    }

    // B-Q4
    @Test
    void saveAllowsTwoCustomersWithTheSameNip() {
        Customer first = customerRepository.saveAndFlush(
                new Customer("Jan", "Kowalski", EMAIL, "+48 600 100 200", "1234567890"));
        Customer second = customerRepository.saveAndFlush(
                new Customer("Anna", "Nowak", OTHER_EMAIL, "600-100-201", "1234567890"));
        entityManager.clear();

        assertThat(customerRepository.findById(first.getId()).orElseThrow().getNip()).isEqualTo("1234567890");
        assertThat(customerRepository.findById(second.getId()).orElseThrow().getNip()).isEqualTo("1234567890");
    }
}
