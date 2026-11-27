package com.ecommerce.customerorder.service;

import com.ecommerce.customerorder.domain.Customer;
import com.ecommerce.customerorder.exception.ConflictException;
import com.ecommerce.customerorder.exception.NotFoundException;
import com.ecommerce.customerorder.exception.UnauthorizedException;
import com.ecommerce.customerorder.repository.CustomerRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BL-1 / BL-2: Customer registration, credential validation, profile read and update.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    // ---- BL-1: registration & credential validation ----

    @Transactional
    public Customer register(String name, String username, String rawPassword, String phoneNumber) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new IllegalArgumentException("Password must not be blank");
        }
        if (customerRepository.existsByUsername(username)) {
            throw new ConflictException("Username already taken: " + username);
        }
        String encoded = passwordEncoder.encode(rawPassword);
        return customerRepository.save(new Customer(name, username, encoded, phoneNumber));
    }

    @Transactional(readOnly = true)
    public Customer validateCredentials(String username, String rawPassword) {
        Customer customer = customerRepository.findByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, customer.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        return customer;
    }

    // ---- BL-2: profile read & update ----

    @Transactional(readOnly = true)
    public Customer getById(UUID customerId) {
        return customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found: " + customerId));
    }

    @Transactional
    public Customer update(UUID customerId, String name, String phoneNumber) {
        Customer customer = getById(customerId);
        if (name != null && !name.isBlank()) {
            customer.setName(name);
        }
        if (phoneNumber != null) {
            customer.setPhoneNumber(phoneNumber);
        }
        return customerRepository.save(customer);
    }
}
