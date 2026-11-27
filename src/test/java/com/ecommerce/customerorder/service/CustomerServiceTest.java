package com.ecommerce.customerorder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.customerorder.domain.Customer;
import com.ecommerce.customerorder.exception.ConflictException;
import com.ecommerce.customerorder.exception.NotFoundException;
import com.ecommerce.customerorder.exception.UnauthorizedException;
import com.ecommerce.customerorder.repository.CustomerRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private CustomerService customerService;

    private Customer alice;

    @BeforeEach
    void setUp() {
        alice = new Customer("Alice", "alice", "$2a$encoded", "0123456789");
    }

    // ---- registration ----

    @Test
    void registersNewCustomerSuccessfully() {
        when(customerRepository.existsByUsername("alice")).thenReturn(false);
        when(passwordEncoder.encode("secret")).thenReturn("$2a$encoded");
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer result = customerService.register("Alice", "alice", "secret", "0123456789");

        assertThat(result.getName()).isEqualTo("Alice");
        assertThat(result.getUsername()).isEqualTo("alice");
        assertThat(result.getPassword()).isEqualTo("$2a$encoded");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void registrationFailsOnDuplicateUsername() {
        when(customerRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> customerService.register("Alice", "alice", "secret", null))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("alice");

        verify(customerRepository, never()).save(any());
    }

    @Test
    void registrationFailsOnBlankName() {
        assertThatThrownBy(() -> customerService.register("  ", "alice", "secret", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrationFailsOnBlankUsername() {
        assertThatThrownBy(() -> customerService.register("Alice", "", "secret", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registrationFailsOnBlankPassword() {
        assertThatThrownBy(() -> customerService.register("Alice", "alice", "   ", null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- credential validation ----

    @Test
    void validateCredentialsSucceedsWithCorrectPassword() {
        when(customerRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(passwordEncoder.matches("secret", "$2a$encoded")).thenReturn(true);

        Customer result = customerService.validateCredentials("alice", "secret");

        assertThat(result.getUsername()).isEqualTo("alice");
    }

    @Test
    void validateCredentialsFailsWithWrongPassword() {
        when(customerRepository.findByUsername("alice")).thenReturn(Optional.of(alice));
        when(passwordEncoder.matches("wrong", "$2a$encoded")).thenReturn(false);

        assertThatThrownBy(() -> customerService.validateCredentials("alice", "wrong"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void validateCredentialsFailsForUnknownUsername() {
        when(customerRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.validateCredentials("nobody", "secret"))
                .isInstanceOf(UnauthorizedException.class);
    }

    // ---- profile ----

    @Test
    void getByIdReturnsCustomer() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.of(alice));

        assertThat(customerService.getById(id)).isEqualTo(alice);
    }

    @Test
    void getByIdThrowsNotFoundForUnknownId() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerService.getById(id))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateChangesNameAndPhone() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.of(alice));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer updated = customerService.update(id, "Alicia", "9999999999");

        assertThat(updated.getName()).isEqualTo("Alicia");
        assertThat(updated.getPhoneNumber()).isEqualTo("9999999999");
    }

    @Test
    void updateIgnoresBlankName() {
        UUID id = UUID.randomUUID();
        when(customerRepository.findById(id)).thenReturn(Optional.of(alice));
        when(customerRepository.save(any(Customer.class))).thenAnswer(inv -> inv.getArgument(0));

        Customer updated = customerService.update(id, "  ", "9999999999");

        assertThat(updated.getName()).isEqualTo("Alice"); // unchanged
    }
}
