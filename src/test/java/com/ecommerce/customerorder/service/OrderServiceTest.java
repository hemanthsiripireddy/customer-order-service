package com.ecommerce.customerorder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.customerorder.domain.Customer;
import com.ecommerce.customerorder.domain.CustomerAddress;
import com.ecommerce.customerorder.domain.Order;
import com.ecommerce.customerorder.domain.OrderItem;
import com.ecommerce.customerorder.domain.OrderStatus;
import com.ecommerce.customerorder.exception.ConflictException;
import com.ecommerce.customerorder.exception.ForbiddenException;
import com.ecommerce.customerorder.exception.NotFoundException;
import com.ecommerce.customerorder.external.inventory.InventoryServiceClient;
import com.ecommerce.customerorder.repository.OrderRepository;
import com.ecommerce.customerorder.service.OrderService.OrderItemRequest;
import java.math.BigDecimal;
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
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private CustomerService customerService;
    @Mock
    private AddressService addressService;
    @Mock
    private InventoryServiceClient inventoryServiceClient;

    @InjectMocks
    private OrderService orderService;

    private Customer customer;
    private CustomerAddress address;
    private UUID customerId;
    private UUID addressId;
    private UUID orderId;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        orderId = UUID.randomUUID();

        customer = new Customer("Alice", "alice", "hash", null);
        address = new CustomerAddress(customer, "1 Main St", "Springfield", "IL", "62701");
    }

    // ---- place order ----

    @Test
    void placesOrderSuccessfully() {
        when(customerService.getById(customerId)).thenReturn(customer);
        when(addressService.getActiveAddressForOrder(customerId, addressId)).thenReturn(address);
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(UUID.randomUUID(), 2, new BigDecimal("19.99")),
                new OrderItemRequest(UUID.randomUUID(), 1, new BigDecimal("5.00")));

        Order order = orderService.placeOrder(customerId, addressId, items);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(order.getTotalPrice()).isEqualByComparingTo("44.98"); // 2*19.99 + 1*5.00
        assertThat(order.getItems()).hasSize(2);
        verify(inventoryServiceClient).reserve(any());
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void placeOrderFailsWithNoItems() {
        when(customerService.getById(customerId)).thenReturn(customer);
        when(addressService.getActiveAddressForOrder(customerId, addressId)).thenReturn(address);

        assertThatThrownBy(() -> orderService.placeOrder(customerId, addressId, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least one item");
    }

    @Test
    void placeOrderFailsWithZeroQuantity() {
        when(customerService.getById(customerId)).thenReturn(customer);
        when(addressService.getActiveAddressForOrder(customerId, addressId)).thenReturn(address);

        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(UUID.randomUUID(), 0, new BigDecimal("10.00")));

        assertThatThrownBy(() -> orderService.placeOrder(customerId, addressId, items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Quantity must be positive");
    }

    @Test
    void placeOrderFailsWithNegativeUnitPrice() {
        when(customerService.getById(customerId)).thenReturn(customer);
        when(addressService.getActiveAddressForOrder(customerId, addressId)).thenReturn(address);

        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(UUID.randomUUID(), 1, new BigDecimal("-5.00")));

        assertThatThrownBy(() -> orderService.placeOrder(customerId, addressId, items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unit price must be positive");
    }

    // ---- retrieval ----

    @Test
    void getOrderReturnsOrderById() {
        Order order = new Order(customer, address, BigDecimal.TEN, OrderStatus.PLACED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThat(orderService.getOrder(orderId)).isEqualTo(order);
    }

    @Test
    void getOrderThrowsNotFoundForUnknownId() {
        when(orderRepository.findById(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(orderId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getOrderForCustomerRejectsCrossCustomerAccess() {
        Customer other = new Customer("Bob", "bob", "hash", null);
        Order order = new Order(other, address, BigDecimal.TEN, OrderStatus.PLACED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderForCustomer(customerId, orderId))
                .isInstanceOf(ForbiddenException.class);
    }

    // ---- cancellation ----

    @Test
    void cancelsPlacedOrder() {
        Order order = new Order(customer, address, BigDecimal.TEN, OrderStatus.PLACED);
        order.addItem(new OrderItem(UUID.randomUUID(), 1, BigDecimal.TEN));

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        // getOrderForCustomer checks customer ownership — customer id matches null in unit test
        // We stub findById directly and bypass the ownership check by using the same customer
        UUID cid = order.getCustomer().getCustomerId(); // null in unit test, same object reference
        Order cancelled = orderService.cancelOrder(cid, orderId);

        assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(inventoryServiceClient).release(any());
    }

    @Test
    void cancelDeliveredOrderFails() {
        Order order = new Order(customer, address, BigDecimal.TEN, OrderStatus.DELIVERED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        UUID cid = order.getCustomer().getCustomerId();
        assertThatThrownBy(() -> orderService.cancelOrder(cid, orderId))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("DELIVERED");
    }

    // ---- status transitions ----

    @Test
    void updatesStatusFromPlacedToDelivered() {
        Order order = new Order(customer, address, BigDecimal.TEN, OrderStatus.PLACED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        Order updated = orderService.updateStatus(orderId, OrderStatus.DELIVERED);

        assertThat(updated.getStatus()).isEqualTo(OrderStatus.DELIVERED);
    }

    @Test
    void rejectsInvalidStatusTransition() {
        Order order = new Order(customer, address, BigDecimal.TEN, OrderStatus.CANCELLED);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateStatus(orderId, OrderStatus.PLACED))
                .isInstanceOf(ConflictException.class);
    }
}
