package com.ecommerce.customerorder.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ecommerce.customerorder.domain.OrderStatus;
import com.ecommerce.customerorder.exception.ConflictException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class OrderStatusTransitionTest {

    @ParameterizedTest(name = "{0} → {1} should be allowed")
    @CsvSource({
        "PLACED, CANCELLED",
        "PLACED, REJECTED",
        "PLACED, DELIVERED",
        "DELIVERED, RETURNED",
        "DELIVERED, PARTIALLY_RETURNED",
        "PARTIALLY_RETURNED, RETURNED"
    })
    void allowsValidTransitions(OrderStatus from, OrderStatus to) {
        assertThat(OrderStatusTransition.canTransition(from, to)).isTrue();
    }

    @ParameterizedTest(name = "{0} → {1} should be rejected")
    @CsvSource({
        "CANCELLED, PLACED",
        "CANCELLED, DELIVERED",
        "REJECTED, PLACED",
        "DELIVERED, PLACED",
        "DELIVERED, CANCELLED",
        "RETURNED, PLACED",
        "RETURNED, DELIVERED",
        "PLACED, RETURNED",
        "PLACED, PARTIALLY_RETURNED"
    })
    void rejectsInvalidTransitions(OrderStatus from, OrderStatus to) {
        assertThat(OrderStatusTransition.canTransition(from, to)).isFalse();
    }

    @Test
    void assertCanTransitionThrowsConflictExceptionOnInvalidTransition() {
        assertThatThrownBy(() ->
                OrderStatusTransition.assertCanTransition(OrderStatus.CANCELLED, OrderStatus.PLACED))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("CANCELLED")
                .hasMessageContaining("PLACED");
    }

    @Test
    void assertCanTransitionDoesNotThrowOnValidTransition() {
        // should not throw
        OrderStatusTransition.assertCanTransition(OrderStatus.PLACED, OrderStatus.DELIVERED);
    }
}
