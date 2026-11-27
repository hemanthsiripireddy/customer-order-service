package com.ecommerce.customerorder.external.inventory;

import com.ecommerce.customerorder.external.inventory.InventoryServiceClient.ReservationRequest;
import java.util.List;

/**
 * Placeholder implementation of {@link InventoryServiceClient}.
 *
 * <p>This stub always succeeds. It will be replaced when the external Inventory Service
 * communication layer is implemented (BL-5 / BL-7 external phase).
 */
public class StubInventoryServiceClient implements InventoryServiceClient {

    @Override
    public void reserve(List<ReservationRequest> items) {
        // TODO: implement HTTP call to Inventory Service
    }

    @Override
    public void release(List<ReservationRequest> items) {
        // TODO: implement HTTP call to Inventory Service
    }
}
