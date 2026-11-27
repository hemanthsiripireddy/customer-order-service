package com.ecommerce.customerorder.repository;

import com.ecommerce.customerorder.domain.Customer;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {

    Optional<Customer> findByUsername(String username);

    boolean existsByUsername(String username);
}
