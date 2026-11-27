package com.ecommerce.customerorder.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.customerorder.domain.Customer;
import com.ecommerce.customerorder.domain.CustomerAddress;
import com.ecommerce.customerorder.domain.Order;
import com.ecommerce.customerorder.domain.OrderItem;
import com.ecommerce.customerorder.domain.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CustomerAddressRepository addressRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Customer customer;
    private CustomerAddress address;

    @BeforeEach
    void setUp() {
        customer = customerRepository.saveAndFlush(
                new Customer("Alice", "alice", "hash", null));
        address = addressRepository.saveAndFlush(
                new CustomerAddress(customer, "1 Main St", "Springfield", "IL", "62701"));
    }

    @Test
    void persistsOrderWithItemsAndStatus() {
        Order order = new Order(customer, address, new BigDecimal("59.97"), OrderStatus.PLACED);
        order.addItem(new OrderItem(UUID.randomUUID(), 2, new BigDecimal("19.99")));
        order.addItem(new OrderItem(UUID.randomUUID(), 1, new BigDecimal("19.99")));

        Order saved = orderRepository.saveAndFlush(order);
        UUID orderId = saved.getOrderId();

        entityManager.clear();

        Order reloaded = orderRepository.findById(orderId).orElseThrow();
        assertThat(reloaded.getCustomer().getCustomerId()).isEqualTo(customer.getCustomerId());
        assertThat(reloaded.getAddress().getAddressId()).isEqualTo(address.getAddressId());
        assertThat(reloaded.getStatus()).isEqualTo(OrderStatus.PLACED);
        assertThat(reloaded.getTotalPrice()).isEqualByComparingTo("59.97");
        assertThat(reloaded.getItems()).hasSize(2);
        assertThat(reloaded.getItems())
                .allSatisfy(item -> assertThat(item.getUnitPrice()).isEqualByComparingTo("19.99"));
    }

    @Test
    void findsOrdersForCustomer() {
        orderRepository.saveAndFlush(
                new Order(customer, address, new BigDecimal("10.00"), OrderStatus.PLACED));

        Customer other = customerRepository.saveAndFlush(
                new Customer("Bob", "bob", "hash", null));
        CustomerAddress otherAddress = addressRepository.saveAndFlush(
                new CustomerAddress(other, "2 Oak Ave", "Shelbyville", "IL", "62702"));
        orderRepository.saveAndFlush(
                new Order(other, otherAddress, new BigDecimal("20.00"), OrderStatus.PLACED));

        assertThat(orderRepository.findByCustomerCustomerId(customer.getCustomerId()))
                .hasSize(1);
    }

    @Test
    void findsOrderItemsByOrder() {
        Order order = new Order(customer, address, new BigDecimal("10.00"), OrderStatus.PLACED);
        order.addItem(new OrderItem(UUID.randomUUID(), 1, new BigDecimal("10.00")));
        Order saved = orderRepository.saveAndFlush(order);

        assertThat(orderItemRepository.findByOrderOrderId(saved.getOrderId()))
                .hasSize(1);
    }
}
