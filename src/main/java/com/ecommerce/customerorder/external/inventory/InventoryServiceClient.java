package com.ecommerce.customerorder.external.inventory;

import java.util.List;
import java.util.UUID;

/**
 * Contract for communicating with the external Inventory Service.
 *
 * <p>Phase: external-service-communication (to be implemented).
 * The concrete HTTP client will live in this package once the external phase is filled in.
 */
public interface InventoryServiceClient {

    /**
     * Reserves stock for the given items. Throws a runtime exception if any reservation fails.
     */
    void reserve(List<ReservationRequest> items);

    /**
     * Releases previously reserved (or held) stock for the given items.
     */
    void release(List<ReservationRequest> items);

    record ReservationRequest(UUID productId, int quantity) {}
}
