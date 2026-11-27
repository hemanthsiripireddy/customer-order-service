package com.ecommerce.customerorder.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecommerce.customerorder.domain.Customer;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
class CustomerRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Test
    void savesCustomerAndFindsByUsername() {
        Customer saved = customerRepository.saveAndFlush(
                new Customer("Alice", "alice", "{bcrypt}hash", "0123456789"));

        assertThat(saved.getCustomerId()).isNotNull();

        Optional<Customer> found = customerRepository.findByUsername("alice");
        assertThat(found).isPresent();
        assertThat(found.get().getCustomerId()).isEqualTo(saved.getCustomerId());
    }

    @Test
    void reportsWhetherUsernameExists() {
        customerRepository.saveAndFlush(new Customer("Alice", "alice", "hash", null));

        assertThat(customerRepository.existsByUsername("alice")).isTrue();
        assertThat(customerRepository.existsByUsername("bob")).isFalse();
    }

    @Test
    void rejectsDuplicateUsername() {
        customerRepository.saveAndFlush(new Customer("Alice", "alice", "hash", null));

        assertThatThrownBy(() -> customerRepository.saveAndFlush(
                new Customer("Bob", "alice", "hash", null)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
