package com.ecommerce.customerorder.repository;

import com.ecommerce.customerorder.domain.CustomerAddress;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {

    List<CustomerAddress> findByCustomerCustomerId(UUID customerId);

    List<CustomerAddress> findByCustomerCustomerIdAndDeletedFalse(UUID customerId);

    Optional<CustomerAddress> findByAddressIdAndCustomerCustomerId(UUID addressId, UUID customerId);

    Optional<CustomerAddress> findByAddressIdAndCustomerCustomerIdAndDeletedFalse(
            UUID addressId, UUID customerId);
}
