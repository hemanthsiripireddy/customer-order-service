package com.ecommerce.customerorder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.customerorder.domain.Customer;
import com.ecommerce.customerorder.domain.CustomerAddress;
import com.ecommerce.customerorder.exception.ForbiddenException;
import com.ecommerce.customerorder.exception.NotFoundException;
import com.ecommerce.customerorder.repository.CustomerAddressRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AddressServiceTest {

    @Mock
    private CustomerAddressRepository addressRepository;

    @Mock
    private CustomerService customerService;

    @InjectMocks
    private AddressService addressService;

    private Customer customer;
    private UUID customerId;
    private CustomerAddress address;
    private UUID addressId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        addressId = UUID.randomUUID();

        customer = new Customer("Alice", "alice", "hash", null);
        customer.setCustomerId(customerId); // simulate JPA-generated UUID
        address = new CustomerAddress(customer, "1 Main St", "Springfield", "IL", "62701");
    }

    @Test
    void addsAddressForCustomer() {
        when(customerService.getById(customerId)).thenReturn(customer);
        when(addressRepository.save(any(CustomerAddress.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerAddress result = addressService.addAddress(
                customerId, "1 Main St", "Springfield", "IL", "62701");

        assertThat(result.getAddressLine()).isEqualTo("1 Main St");
        assertThat(result.getCity()).isEqualTo("Springfield");
        verify(addressRepository).save(any(CustomerAddress.class));
    }

    @Test
    void listsActiveAddresses() {
        when(customerService.getById(customerId)).thenReturn(customer);
        when(addressRepository.findByCustomerCustomerIdAndDeletedFalse(customerId))
                .thenReturn(List.of(address));

        List<CustomerAddress> results = addressService.listActiveAddresses(customerId);

        assertThat(results).hasSize(1);
    }

    @Test
    void softDeletesAddress() {
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));
        when(addressRepository.save(any(CustomerAddress.class))).thenAnswer(inv -> inv.getArgument(0));

        addressService.softDeleteAddress(customerId, addressId);

        verify(addressRepository).save(address);
        assertThat(address.isDeleted()).isTrue();
    }

    @Test
    void softDeleteRejectsCrossCustomerAccess() {
        Customer other = new Customer("Bob", "bob", "hash", null);
        other.setCustomerId(UUID.randomUUID()); // different UUID from customerId
        CustomerAddress bobAddress = new CustomerAddress(other, "2 Oak Ave", "Shelbyville", "IL", "62702");

        when(addressRepository.findById(addressId)).thenReturn(Optional.of(bobAddress));

        assertThatThrownBy(() -> addressService.softDeleteAddress(customerId, addressId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getAddressReturnsActiveAddress() {
        when(addressRepository.findByAddressIdAndCustomerCustomerIdAndDeletedFalse(addressId, customerId))
                .thenReturn(Optional.of(address));

        CustomerAddress result = addressService.getAddress(customerId, addressId);

        assertThat(result).isEqualTo(address);
    }

    @Test
    void getAddressThrowsNotFoundForDeletedOrMissing() {
        when(addressRepository.findByAddressIdAndCustomerCustomerIdAndDeletedFalse(addressId, customerId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getAddress(customerId, addressId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updatesAddressFields() {
        when(addressRepository.findById(addressId)).thenReturn(Optional.of(address));
        when(addressRepository.save(any(CustomerAddress.class))).thenAnswer(inv -> inv.getArgument(0));

        CustomerAddress result = addressService.updateAddress(customerId, addressId, "99 New Rd", "Chicago", "IL", "60601");

        assertThat(result.getAddressLine()).isEqualTo("99 New Rd");
        assertThat(result.getCity()).isEqualTo("Chicago");
    }
}
