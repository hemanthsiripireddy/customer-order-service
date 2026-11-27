package com.ecommerce.customerorder.repository;

import com.ecommerce.customerorder.domain.Order;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByCustomerCustomerId(UUID customerId);
}
