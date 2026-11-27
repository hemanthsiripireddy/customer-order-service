package com.ecommerce.customerorder.service;

import com.ecommerce.customerorder.domain.Customer;
import com.ecommerce.customerorder.domain.CustomerAddress;
import com.ecommerce.customerorder.exception.ForbiddenException;
import com.ecommerce.customerorder.exception.NotFoundException;
import com.ecommerce.customerorder.repository.CustomerAddressRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BL-3: Address management with soft delete.
 *
 * <p>Decision on address immutability: address rows are mutable (in-place update). Existing orders
 * keep the same {@code address_id} foreign key; the order captures the delivery address at
 * placement time by referencing the row. This is an acknowledged simplification — the HLD flags
 * this as an open decision and implementors must not silently choose a behavior. We record here
 * that a snapshot / versioned-address approach is deferred to a future story.
 */
@Service
@RequiredArgsConstructor
public class AddressService {

    private final CustomerAddressRepository addressRepository;
    private final CustomerService customerService;

    @Transactional
    public CustomerAddress addAddress(UUID customerId, String addressLine, String city,
            String state, String pincode) {
        Customer customer = customerService.getById(customerId);
        CustomerAddress address = new CustomerAddress(customer, addressLine, city, state, pincode);
        return addressRepository.save(address);
    }

    @Transactional(readOnly = true)
    public List<CustomerAddress> listActiveAddresses(UUID customerId) {
        customerService.getById(customerId); // ensure customer exists
        return addressRepository.findByCustomerCustomerIdAndDeletedFalse(customerId);
    }

    @Transactional(readOnly = true)
    public CustomerAddress getAddress(UUID customerId, UUID addressId) {
        return addressRepository
                .findByAddressIdAndCustomerCustomerIdAndDeletedFalse(addressId, customerId)
                .orElseThrow(() -> new NotFoundException(
                        "Address not found: " + addressId + " for customer: " + customerId));
    }

    @Transactional
    public CustomerAddress updateAddress(UUID customerId, UUID addressId,
            String addressLine, String city, String state, String pincode) {
        CustomerAddress address = requireOwned(customerId, addressId);
        if (addressLine != null && !addressLine.isBlank()) {
            address.setAddressLine(addressLine);
        }
        if (city != null && !city.isBlank()) {
            address.setCity(city);
        }
        if (state != null && !state.isBlank()) {
            address.setState(state);
        }
        if (pincode != null && !pincode.isBlank()) {
            address.setPincode(pincode);
        }
        return addressRepository.save(address);
    }

    @Transactional
    public void softDeleteAddress(UUID customerId, UUID addressId) {
        CustomerAddress address = requireOwned(customerId, addressId);
        address.setDeleted(true);
        addressRepository.save(address);
    }

    // ---- internal helpers ----

    /**
     * Loads an address that must belong to the given customer and must not be deleted.
     * Throws {@link NotFoundException} or {@link ForbiddenException} as appropriate.
     */
    private CustomerAddress requireOwned(UUID customerId, UUID addressId) {
        CustomerAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new NotFoundException("Address not found: " + addressId));
        if (!address.getCustomer().getCustomerId().equals(customerId)) {
            throw new ForbiddenException(
                    "Address " + addressId + " does not belong to customer " + customerId);
        }
        return address;
    }

    /**
     * Returns active address suitable for order placement; throws if deleted or not owned.
     */
    @Transactional(readOnly = true)
    public CustomerAddress getActiveAddressForOrder(UUID customerId, UUID addressId) {
        return addressRepository
                .findByAddressIdAndCustomerCustomerIdAndDeletedFalse(addressId, customerId)
                .orElseThrow(() -> new NotFoundException(
                        "Address not available for order: " + addressId));
    }
}
