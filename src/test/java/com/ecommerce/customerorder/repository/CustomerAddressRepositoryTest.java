package com.ecommerce.customerorder.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.customerorder.domain.Customer;
import com.ecommerce.customerorder.domain.CustomerAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class CustomerAddressRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerAddressRepository addressRepository;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = customerRepository.saveAndFlush(
                new Customer("Alice", "alice", "hash", null));
    }

    @Test
    void findsActiveAddressesForCustomer() {
        addressRepository.saveAndFlush(
                new CustomerAddress(customer, "1 Main St", "Springfield", "IL", "62701"));

        assertThat(addressRepository.findByCustomerCustomerIdAndDeletedFalse(customer.getCustomerId()))
                .hasSize(1);
        assertThat(addressRepository.findByCustomerCustomerId(customer.getCustomerId()))
                .hasSize(1);
    }

    @Test
    void softDeletedAddressIsExcludedFromActiveQueriesButStillRetrievable() {
        CustomerAddress address = addressRepository.saveAndFlush(
                new CustomerAddress(customer, "1 Main St", "Springfield", "IL", "62701"));

        address.setDeleted(true);
        addressRepository.saveAndFlush(address);

        assertThat(addressRepository.findByCustomerCustomerIdAndDeletedFalse(customer.getCustomerId()))
                .isEmpty();
        assertThat(addressRepository.findByCustomerCustomerId(customer.getCustomerId()))
                .hasSize(1);
        assertThat(addressRepository.findByAddressIdAndCustomerCustomerId(
                address.getAddressId(), customer.getCustomerId()))
                .isPresent();
    }

    @Test
    void doesNotReturnAddressBelongingToAnotherCustomer() {
        Customer other = customerRepository.saveAndFlush(
                new Customer("Bob", "bob", "hash", null));
        CustomerAddress bobAddress = addressRepository.saveAndFlush(
                new CustomerAddress(other, "2 Oak Ave", "Shelbyville", "IL", "62702"));

        assertThat(addressRepository.findByAddressIdAndCustomerCustomerId(
                bobAddress.getAddressId(), customer.getCustomerId()))
                .isEmpty();
        assertThat(addressRepository.findByAddressIdAndCustomerCustomerIdAndDeletedFalse(
                bobAddress.getAddressId(), customer.getCustomerId()))
                .isEmpty();
    }
}
