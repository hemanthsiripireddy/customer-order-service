package com.ecommerce.customerorder.service;

import com.ecommerce.customerorder.domain.OrderStatus;
import com.ecommerce.customerorder.exception.ConflictException;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * BL-8: Centralised order status transition guard.
 *
 * <p>Allowed transitions:
 * <pre>
 *   PLACED           → CANCELLED, REJECTED, DELIVERED
 *   CANCELLED        → (terminal)
 *   REJECTED         → (terminal)
 *   DELIVERED        → RETURNED, PARTIALLY_RETURNED
 *   RETURNED         → (terminal)
 *   PARTIALLY_RETURNED → RETURNED
 * </pre>
 */
public final class OrderStatusTransition {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED =
            new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(OrderStatus.PLACED,
                EnumSet.of(OrderStatus.CANCELLED, OrderStatus.REJECTED, OrderStatus.DELIVERED));
        ALLOWED.put(OrderStatus.CANCELLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.REJECTED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.DELIVERED,
                EnumSet.of(OrderStatus.RETURNED, OrderStatus.PARTIALLY_RETURNED));
        ALLOWED.put(OrderStatus.RETURNED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(OrderStatus.PARTIALLY_RETURNED, EnumSet.of(OrderStatus.RETURNED));
    }

    private OrderStatusTransition() {}

    public static boolean canTransition(OrderStatus from, OrderStatus to) {
        Set<OrderStatus> targets = ALLOWED.get(from);
        return targets != null && targets.contains(to);
    }

    public static void assertCanTransition(OrderStatus from, OrderStatus to) {
        if (!canTransition(from, to)) {
            throw new ConflictException(
                    "Cannot transition order from " + from + " to " + to);
        }
    }
}
