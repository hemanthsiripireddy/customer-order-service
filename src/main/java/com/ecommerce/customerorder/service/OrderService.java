package com.ecommerce.customerorder.service;

import com.ecommerce.customerorder.domain.Customer;
import com.ecommerce.customerorder.domain.CustomerAddress;
import com.ecommerce.customerorder.domain.Order;
import com.ecommerce.customerorder.domain.OrderItem;
import com.ecommerce.customerorder.domain.OrderStatus;
import com.ecommerce.customerorder.exception.ConflictException;
import com.ecommerce.customerorder.exception.ForbiddenException;
import com.ecommerce.customerorder.exception.NotFoundException;
import com.ecommerce.customerorder.external.inventory.InventoryServiceClient;
import com.ecommerce.customerorder.external.inventory.InventoryServiceClient.ReservationRequest;
import com.ecommerce.customerorder.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * BL-4 / BL-5 / BL-6 / BL-7 / BL-8: Order placement, retrieval, cancellation, and status lifecycle.
 */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerService customerService;
    private final AddressService addressService;
    private final InventoryServiceClient inventoryServiceClient;

    // ---- BL-4 / BL-5: order validation, price calculation and placement ----

    /**
     * Places a new order atomically:
     * <ol>
     *   <li>Validate the customer and delivery address.</li>
     *   <li>Validate items (non-empty, positive quantities).</li>
     *   <li>Compute line totals and the order total.</li>
     *   <li>Reserve stock via the Inventory Service.</li>
     *   <li>Persist the order if reservation succeeds.</li>
     * </ol>
     */
    @Transactional
    public Order placeOrder(UUID customerId, UUID addressId, List<OrderItemRequest> itemRequests) {
        // validate customer & address
        Customer customer = customerService.getById(customerId);
        CustomerAddress address = addressService.getActiveAddressForOrder(customerId, addressId);

        // validate items
        if (itemRequests == null || itemRequests.isEmpty()) {
            throw new IllegalArgumentException("Order must contain at least one item");
        }
        for (OrderItemRequest req : itemRequests) {
            if (req.quantity() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be positive for product: " + req.productId());
            }
            if (req.unitPrice() == null || req.unitPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Unit price must be positive for product: " + req.productId());
            }
        }

        // compute totals
        BigDecimal total = itemRequests.stream()
                .map(r -> r.unitPrice().multiply(BigDecimal.valueOf(r.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // reserve inventory (external call — if this fails the transaction rolls back)
        List<ReservationRequest> reservations = itemRequests.stream()
                .map(r -> new ReservationRequest(r.productId(), r.quantity()))
                .toList();
        inventoryServiceClient.reserve(reservations);

        // persist
        Order order = new Order(customer, address, total, OrderStatus.PLACED);
        for (OrderItemRequest req : itemRequests) {
            order.addItem(new OrderItem(req.productId(), req.quantity(), req.unitPrice()));
        }
        return orderRepository.save(order);
    }

    // ---- BL-6: order retrieval ----

    @Transactional(readOnly = true)
    public Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId));
    }

    @Transactional(readOnly = true)
    public Order getOrderForCustomer(UUID customerId, UUID orderId) {
        Order order = getOrder(orderId);
        if (!order.getCustomer().getCustomerId().equals(customerId)) {
            throw new ForbiddenException(
                    "Order " + orderId + " does not belong to customer " + customerId);
        }
        return order;
    }

    @Transactional(readOnly = true)
    public List<Order> listOrdersForCustomer(UUID customerId) {
        customerService.getById(customerId); // ensure customer exists
        return orderRepository.findByCustomerCustomerId(customerId);
    }

    // ---- BL-7: cancellation ----

    /**
     * Cancels an eligible order:
     * <ul>
     *   <li>Only {@code PLACED} orders may be cancelled (pre-dispatch).</li>
     *   <li>Releases reserved stock via the Inventory Service.</li>
     * </ul>
     * <p>Note: cancellation eligibility after dispatch (e.g. while in transit) is a
     * {@code Decision required} item in the HLD and is deferred to a future story.
     */
    @Transactional
    public Order cancelOrder(UUID customerId, UUID orderId) {
        Order order = getOrderForCustomer(customerId, orderId);
        OrderStatusTransition.assertCanTransition(order.getStatus(), OrderStatus.CANCELLED);

        // release inventory
        List<ReservationRequest> releases = order.getItems().stream()
                .map(i -> new ReservationRequest(i.getProductId(), i.getQuantity()))
                .toList();
        inventoryServiceClient.release(releases);

        order.setStatus(OrderStatus.CANCELLED);
        return orderRepository.save(order);
    }

    // ---- BL-8: status transition ----

    @Transactional
    public Order updateStatus(UUID orderId, OrderStatus newStatus) {
        Order order = getOrder(orderId);
        OrderStatusTransition.assertCanTransition(order.getStatus(), newStatus);
        order.setStatus(newStatus);
        return orderRepository.save(order);
    }

    // ---- inner request type ----

    public record OrderItemRequest(UUID productId, int quantity, BigDecimal unitPrice) {}
}
